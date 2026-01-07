package com.club.site.post.service;

import com.club.site.common.dto.PagedResult;
import com.club.site.common.error.ErrorCode;
import com.club.site.common.exception.BusinessException;
import com.club.site.common.service.AuditLogService;
import com.club.site.member.service.MemberService;
import com.club.site.post.dto.ImageInfo;
import com.club.site.post.dto.PostCreateRequest;
import com.club.site.post.dto.PostDto;
import com.club.site.post.dto.PostListItemDTO;
import com.club.site.post.dto.PostListResponse;
import com.club.site.post.dto.PostUpdateRequest;
import com.club.site.util.CursorCodec;
import com.club.site.util.EventDate;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.UrlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final Firestore firestore;
    private final MemberService memberService;
    private final AuditLogService auditLogService;

    public PostService(Firestore firestore, MemberService memberService, AuditLogService auditLogService) {
        this.firestore = firestore;
        this.memberService = memberService;
        this.auditLogService = auditLogService;
    }

    /**
     * 게시글 리스트 조회 (Public)
     * 경량 DTO(PostListItemDTO) 반환
     */
    public PostListResponse listPosts(Integer pageSize, String cursor) throws Exception {
        int size = sanitizePageSize(pageSize);

        Query q = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId());

        CursorCodec.Cursor decoded = CursorCodec.decodeOrNull(cursor);
        if (decoded != null) {
            Timestamp ts = FirestoreUtils.fromMillis(decoded.tsMillis());
            if (ts != null) {
                q = q.startAfter(ts, decoded.id());
            }
        }

        List<QueryDocumentSnapshot> docs = q.limit(size + 1).get().get().getDocuments();
        boolean hasMore = docs.size() > size;
        List<? extends DocumentSnapshot> page = hasMore ? docs.subList(0, size) : docs;

        // PostDto로 조회한 후 PostListItemDTO로 변환
        List<PostListItemDTO> items = new ArrayList<>();
        for (DocumentSnapshot doc : page) {
            PostDto postDto = toDto(doc);
            items.add(convertToListItem(postDto));
        }

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }
        return new PostListResponse(items, nextCursor);
    }

    public PostDto getPost(String postId) throws Exception {
        DocumentSnapshot doc = firestore.collection("posts").document(postId).get().get();
        if (!doc.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }
        return toDto(doc);
    }

    /**
     * 게시글 생성 (Admin)
     * @param actorUid 작성자 UID
     * @param request 게시글 생성 요청
     * @return 생성된 게시글 DTO
     */
    public PostDto createPost(String actorUid, PostCreateRequest request) throws Exception {
        // 1. Validation
        validateCreateRequest(request);
        
        // 2. eventDate 처리 (optional)
        String normalizedDate = normalizeEventDateOrNull(request.eventDate());
        
        // 3. images 처리 (ImageInfo 리스트)
        List<ImageInfo> images = sanitizeImages(request.images());
        if (images.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10장까지 업로드 가능합니다.");
        }
        String thumbnailUrl = images.isEmpty() ? null : images.get(0).url();

        // 4. authorName 조회 (스냅샷 저장)
        String authorName;
        try {
            authorName = memberService.getMemberByUid(actorUid).name();
        } catch (Exception e) {
            authorName = "Unknown";
        }

        // 5. Firestore에 저장 (ImageInfo를 Map으로 변환)
        Timestamp now = Timestamp.now();
        List<Map<String, Object>> imagesData = images.stream()
                .map(img -> Map.<String, Object>of("url", img.url(), "path", img.path()))
                .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title().trim());
        data.put("body", request.body().trim());
        data.put("eventDate", normalizedDate); // null 허용
        data.put("images", imagesData); // List<Map<String, Object>>
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("authorId", actorUid);
        data.put("authorName", authorName); // 스냅샷 저장
        data.put("createdAt", now);
        data.put("updatedAt", now);

        DocumentReference ref = firestore.collection("posts").document();
        ref.set(data).get();
        auditLogService.write("POST_CREATE", actorUid, ref.getId(), Map.of("title", request.title()));
        return toDto(ref.get().get());
    }
    
    /**
     * eventDate를 정규화하거나 null 반환 (optional 처리)
     */
    private String normalizeEventDateOrNull(String eventDate) {
        if (eventDate == null || eventDate.isBlank()) {
            return null;
        }
        try {
            return EventDate.normalize(eventDate);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, 
                    "eventDate는 YYYY-MM-DD 형식이어야 합니다: " + eventDate);
        }
    }
    
    /**
     * 게시글 생성 요청 Validation
     */
    private void validateCreateRequest(PostCreateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "제목은 필수입니다.");
        }
        if (request.title().length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "제목은 최대 100자까지 입력 가능합니다.");
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "내용은 필수입니다.");
        }
        if (request.images() != null && request.images().size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10장까지 업로드 가능합니다.");
        }
    }

    /**
     * 게시글 수정 (로그인 필요, 본인 게시글만 수정 가능)
     * @param actorUid 수정자 UID
     * @param postId 게시글 ID
     * @param request 게시글 수정 요청
     * @return 수정된 게시글 DTO
     */
    public PostDto updatePost(String actorUid, String postId, PostUpdateRequest request) throws Exception {
        DocumentReference ref = firestore.collection("posts").document(postId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }
        
        // 본인 게시글만 수정 가능
        String authorId = existing.getString("authorId");
        if (authorId == null || !authorId.equals(actorUid)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED, "본인의 게시글만 수정할 수 있습니다.");
        }

        // 1. Validation
        validateUpdateRequest(request);
        
        // 2. eventDate 처리 (optional)
        String normalizedDate = normalizeEventDateOrNull(request.eventDate());
        
        // 3. images 처리 (전체 덮어쓰기)
        List<ImageInfo> images = sanitizeImages(request.images());
        if (images.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10장까지 업로드 가능합니다.");
        }
        
        // 4. thumbnailUrl 재계산 (images[0] 기준)
        String thumbnailUrl = images.isEmpty() ? null : images.get(0).url();
        
        // 5. 기존 이미지와 비교하여 삭제 대상 확인 및 Storage에서 삭제
        List<ImageInfo> oldImages = convertToImageInfoList(existing.get("images"));
        deleteRemovedImages(oldImages, images);

        // 6. Firestore 업데이트 (authorId/authorName은 변경하지 않음)
        List<Map<String, Object>> imagesData = images.stream()
                .map(img -> Map.<String, Object>of("url", img.url(), "path", img.path()))
                .collect(Collectors.toList());
        
        Map<String, Object> update = new HashMap<>();
        update.put("title", request.title().trim());
        update.put("body", request.body().trim());
        update.put("eventDate", normalizedDate); // null 허용
        update.put("images", imagesData); // 전체 덮어쓰기
        update.put("thumbnailUrl", thumbnailUrl); // 재계산
        update.put("updatedAt", Timestamp.now());

        ref.update(update).get();
        auditLogService.write("POST_UPDATE", actorUid, postId, Map.of("title", request.title()));
        return toDto(ref.get().get());
    }
    
    /**
     * 게시글 수정 요청 Validation
     */
    private void validateUpdateRequest(PostUpdateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "제목은 필수입니다.");
        }
        if (request.title().length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "제목은 최대 100자까지 입력 가능합니다.");
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "내용은 필수입니다.");
        }
        if (request.images() != null && request.images().size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10장까지 업로드 가능합니다.");
        }
    }

    /**
     * 게시글 삭제 (로그인 필요, 본인 게시글만 삭제 가능, 하드 삭제)
     * Storage 이미지도 함께 삭제
     */
    public void deletePost(String actorUid, String postId) throws Exception {
        DocumentReference ref = firestore.collection("posts").document(postId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND);
        }
        
        // 본인 게시글만 삭제 가능
        String authorId = existing.getString("authorId");
        if (authorId == null || !authorId.equals(actorUid)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED, "본인의 게시글만 삭제할 수 있습니다.");
        }
        
        // 1. 이미지 Storage에서 삭제
        List<ImageInfo> images = convertToImageInfoList(existing.get("images"));
        deleteImagesFromStorage(images);
        
        // 2. Firestore 문서 삭제
        ref.delete().get();
        auditLogService.write("POST_DELETE", actorUid, postId, Map.of());
    }

    private static PostDto toDto(DocumentSnapshot doc) {
        return new PostDto(
                doc.getId(),
                doc.getString("title"),
                doc.getString("body"),
                doc.getString("eventDate"),
                convertToImageInfoList(doc.get("images")), // List<ImageInfo>로 변환
                doc.getString("thumbnailUrl"),
                doc.getString("authorId"),
                doc.getString("authorName"),
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }

    /**
     * Firestore의 images 필드를 List<ImageInfo>로 변환
     */
    private static List<ImageInfo> convertToImageInfoList(Object imagesData) {
        if (imagesData == null) {
            return List.of();
        }
        
        List<ImageInfo> result = new ArrayList<>();
        
        // List<Map<String, Object>> 형태인 경우 (새 구조)
        if (imagesData instanceof List) {
            List<?> list = (List<?>) imagesData;
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    String url = String.valueOf(map.getOrDefault("url", ""));
                    String path = String.valueOf(map.getOrDefault("path", ""));
                    if (!url.isBlank()) {
                        result.add(new ImageInfo(url, path));
                    }
                } else if (item instanceof String) {
                    // 기존 구조 (String만 있는 경우) - 하위 호환성
                    String url = (String) item;
                    if (!url.isBlank()) {
                        result.add(new ImageInfo(url, "")); // path는 빈 문자열
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * ImageInfo 리스트 검증 및 정리
     * 
     * 검증 규칙:
     * - images[].url: 필수, HTTP/HTTPS URL 형식만 허용
     * - images[].path: optional, Storage 내부 상대 경로만 허용 (URL 형식 불가)
     * - null/빈 문자열 항목은 제외 처리
     * 
     * 참고:
     * - 이미지 파일 타입(jpg/png/webp) 검증은 클라이언트의 Firebase Storage 업로드 단계에서 처리
     * - 서버는 이미지 파일 자체를 검증하지 않으며, images[].url/path 형식만 검증
     */
    private static List<ImageInfo> sanitizeImages(List<ImageInfo> images) {
        if (images == null) {
            return List.of();
        }
        List<ImageInfo> out = new ArrayList<>();
        for (ImageInfo image : images) {
            if (image == null || image.url() == null || image.url().isBlank()) {
                continue;
            }
            // URL 검증 (HTTP/HTTPS 형식만 허용)
            UrlUtils.requireHttpUrl(image.url(), "images.url");
            // path는 optional이지만, 있으면 검증 (Storage 내부 경로만 허용)
            if (image.path() != null && !image.path().isBlank()) {
                // path는 상대 경로여야 함 (예: "posts/abc/image.jpg")
                if (image.path().startsWith("http://") || image.path().startsWith("https://")) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT, 
                            "images.path는 Storage 내부 경로여야 합니다. URL이 아닙니다: " + image.path());
                }
            }
            out.add(new ImageInfo(image.url().trim(), image.path() != null ? image.path().trim() : ""));
        }
        return out;
    }
    
    /**
     * 삭제된 이미지를 Storage에서 제거
     * @param oldImages 기존 이미지 리스트
     * @param newImages 새로운 이미지 리스트
     */
    private void deleteRemovedImages(List<ImageInfo> oldImages, List<ImageInfo> newImages) {
        if (oldImages == null || oldImages.isEmpty()) {
            return;
        }
        
        // 새로운 이미지의 path 집합
        Set<String> newPaths = newImages.stream()
                .map(ImageInfo::path)
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.toSet());
        
        // 기존 이미지 중 새로운 이미지에 없는 것 찾기
        List<ImageInfo> toDelete = oldImages.stream()
                .filter(oldImg -> {
                    String oldPath = oldImg.path();
                    return oldPath != null && !oldPath.isBlank() && !newPaths.contains(oldPath);
                })
                .collect(Collectors.toList());
        
        // Storage에서 삭제
        if (!toDelete.isEmpty()) {
            deleteImagesFromStorage(toDelete);
        }
    }
    
    /**
     * Storage에서 이미지 파일 삭제
     * 일부 실패해도 치명적 오류로 처리하지 않음 (로그만 남김)
     */
    private void deleteImagesFromStorage(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        
        try {
            // Firebase Storage 클라이언트 가져오기
            var storage = StorageClient.getInstance();
            var bucket = storage.bucket();
            
            for (ImageInfo image : images) {
                String path = image.path();
                if (path == null || path.isBlank()) {
                    log.warn("이미지 path가 없어서 삭제할 수 없습니다. url: {}", image.url());
                    continue;
                }
                
                try {
                    // Storage에서 파일 삭제
                    bucket.get(path).delete();
                    log.info("Storage 이미지 삭제 성공: {}", path);
                } catch (Exception e) {
                    // 일부 이미지가 이미 존재하지 않는 경우 치명적 오류로 처리하지 않음
                    log.warn("Storage 이미지 삭제 실패 (무시됨): path={}, error={}", path, e.getMessage());
                }
            }
        } catch (Exception e) {
            // Storage 초기화 실패 등은 로그만 남기고 계속 진행
            log.warn("Storage 클라이언트 초기화 실패 (이미지 삭제 건너뜀): {}", e.getMessage());
        }
    }

    /**
     * PostDto를 PostListItemDTO로 변환
     */
    private static PostListItemDTO convertToListItem(PostDto postDto) {
        return new PostListItemDTO(
                postDto.id(),
                postDto.title(),
                postDto.eventDate(), // null 허용
                postDto.thumbnailUrl(),
                postDto.authorId(),
                postDto.authorName(),
                postDto.createdAt(),
                postDto.updatedAt()
        );
    }

    private int sanitizePageSize(Integer pageSize) {
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, 
                    "pageSize는 1 이상 " + MAX_PAGE_SIZE + " 이하여야 합니다. 현재 값: " + size);
        }
        return size;
    }
}

