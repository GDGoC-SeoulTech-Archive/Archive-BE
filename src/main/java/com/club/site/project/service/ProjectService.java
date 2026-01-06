package com.club.site.project.service;

import com.club.site.common.error.ErrorCode;
import com.club.site.common.exception.BusinessException;
import com.club.site.common.service.AuditLogService;
import com.club.site.member.service.MemberService;
import com.club.site.project.dto.ImageInfo;
import com.club.site.project.dto.ProjectCreateRequest;
import com.club.site.project.dto.ProjectDto;
import com.club.site.project.dto.ProjectListItemDTO;
import com.club.site.project.dto.ProjectListResponse;
import com.club.site.project.dto.ProjectUpdateRequest;
import com.club.site.util.CursorCodec;
import com.club.site.util.EventDate;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.UrlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "png", "webp");

    private final Firestore firestore;
    private final MemberService memberService;
    private final AuditLogService auditLogService;

    public ProjectService(Firestore firestore, MemberService memberService, AuditLogService auditLogService) {
        this.firestore = firestore;
        this.memberService = memberService;
        this.auditLogService = auditLogService;
    }

    /**
     * 프로젝트 목록 조회 (Public)
     */
    public ProjectListResponse listProjects(Integer pageSize, String cursor) throws Exception {
        int size = sanitizePageSize(pageSize);

        Query q = firestore.collection("projects")
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

        List<ProjectListItemDTO> items = new ArrayList<>();
        for (DocumentSnapshot doc : page) {
            ProjectDto projectDto = toDto(doc);
            items.add(convertToListItem(projectDto));
        }

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }

        return new ProjectListResponse(items, nextCursor);
    }

    public ProjectDto getProject(String projectId) throws Exception {
        DocumentSnapshot doc = firestore.collection("projects").document(projectId).get().get();
        if (!doc.exists()) {
            throw new ApiException("NOT_FOUND", "Project not found", HttpStatus.NOT_FOUND);
        }
        return toDto(doc);
    }

    /**
     * 프로젝트 생성 (Admin)
     */
    public ProjectDto createProject(String actorUid, ProjectCreateRequest request) throws Exception {
        validateCreateRequest(request);

        String normalizedDate = normalizeStartDateOrNull(request.startDate());

        List<ImageInfo> images = sanitizeImages(request.images());
        if (images.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10개까지 등록 가능합니다.");
        }
        String thumbnailUrl = images.isEmpty() ? null : images.get(0).url();

        List<String> skills = sanitizeStringList(request.skills());
        List<String> members = sanitizeStringList(request.members());

        String authorName;
        try {
            authorName = memberService.getMemberByUid(actorUid).name();
        } catch (Exception e) {
            authorName = "Unknown";
        }

        Timestamp now = Timestamp.now();
        List<Map<String, Object>> imagesData = images.stream()
                .map(img -> Map.<String, Object>of("url", img.url(), "path", img.path()))
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("title", request.title().trim());
        data.put("body", request.body().trim());
        data.put("startDate", normalizedDate);
        data.put("images", imagesData);
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("skills", skills);
        data.put("members", members);
        data.put("authorId", actorUid);
        data.put("authorName", authorName);
        data.put("createdAt", now);
        data.put("updatedAt", now);

        DocumentReference ref = firestore.collection("projects").document();
        ref.set(data).get();
        auditLogService.write("PROJECT_CREATE", actorUid, ref.getId(), Map.of("title", request.title()));
        return toDto(ref.get().get());
    }

    /**
     * 프로젝트 수정 (Admin, 전체 덮어쓰기)
     */
    public ProjectDto updateProject(String actorUid, String projectId, ProjectUpdateRequest request) throws Exception {
        DocumentReference ref = firestore.collection("projects").document(projectId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Project not found", HttpStatus.NOT_FOUND);
        }

        validateUpdateRequest(request);

        String normalizedDate = normalizeStartDateOrNull(request.startDate());

        List<ImageInfo> images = sanitizeImages(request.images());
        if (images.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10개까지 등록 가능합니다.");
        }

        String thumbnailUrl = images.isEmpty() ? null : images.get(0).url();

        List<ImageInfo> oldImages = convertToImageInfoList(existing.get("images"));
        deleteRemovedImages(oldImages, images);

        List<String> skills = sanitizeStringList(request.skills());
        List<String> members = sanitizeStringList(request.members());

        List<Map<String, Object>> imagesData = images.stream()
                .map(img -> Map.<String, Object>of("url", img.url(), "path", img.path()))
                .collect(Collectors.toList());

        Map<String, Object> update = new HashMap<>();
        update.put("title", request.title().trim());
        update.put("body", request.body().trim());
        update.put("startDate", normalizedDate);
        update.put("images", imagesData);
        update.put("thumbnailUrl", thumbnailUrl);
        update.put("skills", skills);
        update.put("members", members);
        update.put("updatedAt", Timestamp.now());

        ref.update(update).get();
        auditLogService.write("PROJECT_UPDATE", actorUid, projectId, Map.of("title", request.title()));
        return toDto(ref.get().get());
    }

    /**
     * 프로젝트 삭제 (Admin)
     */
    public void deleteProject(String actorUid, String projectId) throws Exception {
        DocumentReference ref = firestore.collection("projects").document(projectId);
        DocumentSnapshot existing = ref.get().get();
        if (!existing.exists()) {
            throw new ApiException("NOT_FOUND", "Project not found", HttpStatus.NOT_FOUND);
        }

        List<ImageInfo> images = convertToImageInfoList(existing.get("images"));
        deleteImagesFromStorage(images);

        ref.delete().get();
        auditLogService.write("PROJECT_DELETE", actorUid, projectId, Map.of());
    }

    private void validateCreateRequest(ProjectCreateRequest request) {
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
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10개까지 등록 가능합니다.");
        }
    }

    private void validateUpdateRequest(ProjectUpdateRequest request) {
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
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이미지는 최대 10개까지 등록 가능합니다.");
        }
    }

    private String normalizeStartDateOrNull(String startDate) {
        if (startDate == null || startDate.isBlank()) {
            return null;
        }
        try {
            return EventDate.normalize(startDate);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "startDate는 YYYY-MM-DD 형식이어야 합니다. 입력값: " + startDate);
        }
    }

    private static ProjectDto toDto(DocumentSnapshot doc) {
        return new ProjectDto(
                doc.getId(),
                doc.getString("title"),
                doc.getString("body"),
                doc.getString("startDate"),
                convertToImageInfoList(doc.get("images")),
                doc.getString("thumbnailUrl"),
                FirestoreUtils.asStringList(doc.get("skills")),
                FirestoreUtils.asStringList(doc.get("members")),
                doc.getString("authorId"),
                doc.getString("authorName"),
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }

    private static ProjectListItemDTO convertToListItem(ProjectDto projectDto) {
        return new ProjectListItemDTO(
                projectDto.id(),
                projectDto.title(),
                projectDto.startDate(),
                projectDto.thumbnailUrl(),
                projectDto.skills(),
                projectDto.authorId(),
                projectDto.authorName(),
                projectDto.createdAt(),
                projectDto.updatedAt()
        );
    }

    private static List<ImageInfo> convertToImageInfoList(Object imagesData) {
        if (imagesData == null) {
            return List.of();
        }

        List<ImageInfo> result = new ArrayList<>();
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
                    String url = (String) item;
                    if (!url.isBlank()) {
                        result.add(new ImageInfo(url, ""));
                    }
                }
            }
        }
        return result;
    }

    private static List<ImageInfo> sanitizeImages(List<ImageInfo> images) {
        if (images == null) {
            return List.of();
        }
        List<ImageInfo> out = new ArrayList<>();
        for (ImageInfo image : images) {
            if (image == null || image.url() == null || image.url().isBlank()) {
                continue;
            }
            String url = image.url().trim();
            UrlUtils.requireHttpUrl(url, "images.url");
            requireAllowedImageExtension(url);

            if (image.path() != null && !image.path().isBlank()) {
                if (image.path().startsWith("http://") || image.path().startsWith("https://")) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                            "images.path는 Storage 내부 경로여야 합니다. URL이 아닙니다: " + image.path());
                }
            }
            out.add(new ImageInfo(url, image.path() != null ? image.path().trim() : ""));
        }
        return out;
    }

    private static void requireAllowedImageExtension(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null) {
                throw new IllegalArgumentException();
            }
            int dot = path.lastIndexOf('.');
            if (dot < 0 || dot == path.length() - 1) {
                throw new IllegalArgumentException();
            }
            String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "images.url은 jpg/png/webp 확장자만 허용합니다: " + url);
        }
    }

    private static List<String> sanitizeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private void deleteRemovedImages(List<ImageInfo> oldImages, List<ImageInfo> newImages) {
        if (oldImages == null || oldImages.isEmpty()) {
            return;
        }

        Set<String> newPaths = newImages.stream()
                .map(ImageInfo::path)
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.toSet());

        List<ImageInfo> toDelete = oldImages.stream()
                .filter(oldImg -> {
                    String oldPath = oldImg.path();
                    return oldPath != null && !oldPath.isBlank() && !newPaths.contains(oldPath);
                })
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            deleteImagesFromStorage(toDelete);
        }
    }

    private void deleteImagesFromStorage(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        try {
            var storage = StorageClient.getInstance();
            var bucket = storage.bucket();

            for (ImageInfo image : images) {
                String path = image.path();
                if (path == null || path.isBlank()) {
                    log.warn("이미지 path가 없어 삭제할 수 없습니다. url: {}", image.url());
                    continue;
                }

                try {
                    bucket.get(path).delete();
                    log.info("Storage 이미지 삭제 완료: {}", path);
                } catch (Exception e) {
                    log.warn("Storage 이미지 삭제 실패(무시): path={}, error={}", path, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Storage 클라이언트 초기화 실패 (이미지 삭제 생략): {}", e.getMessage());
        }
    }

    private int sanitizePageSize(Integer pageSize) {
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "pageSize는 1 이상 " + MAX_PAGE_SIZE + " 이하이어야 합니다. 현재 값: " + size);
        }
        return size;
    }
}
