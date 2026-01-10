package com.club.site.comment.service;

import com.club.site.comment.dto.CommentCreateRequest;
import com.club.site.comment.dto.CommentDto;
import com.club.site.comment.dto.CommentListItemDTO;
import com.club.site.comment.dto.CommentListResponse;
import com.club.site.comment.dto.CommentUpdateRequest;
import com.club.site.common.error.ErrorCode;
import com.club.site.common.exception.BusinessException;
import com.club.site.member.service.MemberService;
import com.club.site.post.service.PostService;
import com.club.site.util.CursorCodec;
import com.club.site.util.FirestoreUtils;
import com.club.site.util.HtmlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CommentService {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final Firestore firestore;
    private final PostService postService;
    private final MemberService memberService;

    public CommentService(Firestore firestore, PostService postService, MemberService memberService) {
        this.firestore = firestore;
        this.postService = postService;
        this.memberService = memberService;
    }

    /**
     * 댓글 리스트 조회 (Public)
     * @param postId 게시글 ID
     * @param pageSize 페이지 크기 (기본 50, 최대 100)
     * @param cursor 커서 (opaque)
     * @return 댓글 리스트 응답
     */
    public CommentListResponse listComments(String postId, Integer pageSize, String cursor) throws Exception {
        // 1. 게시글 존재 확인
        try {
            postService.getPost(postId);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.POST_NOT_FOUND, "게시글을 찾을 수 없습니다: " + postId);
            }
            throw e;
        }

        // 2. pageSize 검증
        int size = sanitizePageSize(pageSize);

        // 3. 서브컬렉션 쿼리 (createdAt ASC 정렬)
        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        Query q = commentsRef
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .orderBy(FieldPath.documentId());

        // 4. Cursor 적용
        CursorCodec.Cursor decoded = CursorCodec.decodeOrNull(cursor);
        if (decoded != null) {
            Timestamp ts = FirestoreUtils.fromMillis(decoded.tsMillis());
            if (ts != null) {
                q = q.startAfter(ts, decoded.id());
            }
        }

        // 5. 페이지네이션
        List<QueryDocumentSnapshot> docs = q.limit(size + 1).get().get().getDocuments();
        boolean hasMore = docs.size() > size;
        List<? extends DocumentSnapshot> page = hasMore ? docs.subList(0, size) : docs;

        // 6. DTO 변환
        List<CommentListItemDTO> items = new ArrayList<>();
        for (DocumentSnapshot doc : page) {
            CommentDto commentDto = toDto(doc, postId);
            items.add(convertToListItem(commentDto));
        }

        // 7. nextCursor 생성
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            DocumentSnapshot last = page.get(page.size() - 1);
            Timestamp ts = last.getTimestamp("createdAt");
            if (ts != null) {
                nextCursor = CursorCodec.encode(new CursorCodec.Cursor(ts.toDate().getTime(), last.getId()));
            }
        }

        return new CommentListResponse(items, nextCursor);
    }

    /**
     * 댓글 생성 (로그인 필요)
     * @param actorUid 작성자 UID
     * @param postId 게시글 ID
     * @param request 댓글 생성 요청
     * @return 생성된 댓글 DTO
     */
    public CommentDto createComment(String actorUid, String postId, CommentCreateRequest request) throws Exception {
        // 1. Validation
        validateCreateRequest(request);

        // 2. 게시글 존재 확인
        try {
            postService.getPost(postId);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.POST_NOT_FOUND, "게시글을 찾을 수 없습니다: " + postId);
            }
            throw e;
        }

        // 3. body 처리 (trim() 후 HTML 이스케이프)
        String body = request.body().trim();
        if (body.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글 내용은 필수입니다.");
        }
        String escapedBody = HtmlUtils.escapeHtml(body);

        // 4. authorName 조회 (스냅샷 저장)
        String authorName;
        try {
            authorName = memberService.getMemberByUid(actorUid).name();
        } catch (Exception e) {
            authorName = "Unknown";
        }

        // 5. Firestore 서브컬렉션에 저장
        Timestamp now = Timestamp.now();
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("body", escapedBody);
        data.put("authorId", actorUid);
        data.put("authorName", authorName); // 스냅샷 저장
        data.put("createdAt", now);
        data.put("updatedAt", now);

        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        DocumentReference ref = commentsRef.document();
        ref.set(data).get();

        return toDto(ref.get().get(), postId);
    }

    /**
     * 댓글 수정 (로그인 필요, 본인 댓글만 수정 가능)
     * @param actorUid 수정자 UID
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     * @param request 댓글 수정 요청
     * @return 수정된 댓글 DTO
     */
    public CommentDto updateComment(String actorUid, String postId, String commentId, CommentUpdateRequest request) throws Exception {
        // 1. 댓글 존재 확인
        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        DocumentReference ref = commentsRef.document(commentId);
        DocumentSnapshot existing = ref.get().get();
        
        if (!existing.exists()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다: " + commentId);
        }

        // 2. 본인 댓글만 수정 가능
        String authorId = existing.getString("authorId");
        if (authorId == null || !authorId.equals(actorUid)) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED, "본인의 댓글만 수정할 수 있습니다.");
        }

        // 3. Validation
        validateUpdateRequest(request);

        // 4. body 처리 (trim() 후 HTML 이스케이프)
        String body = request.body().trim();
        if (body.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글 내용은 필수입니다.");
        }
        String escapedBody = HtmlUtils.escapeHtml(body);

        // 5. Firestore 업데이트 (authorId/authorName은 변경하지 않음)
        Map<String, Object> update = new HashMap<>();
        update.put("body", escapedBody);
        update.put("updatedAt", Timestamp.now());

        ref.update(update).get();
        return toDto(ref.get().get(), postId);
    }

    /**
     * 댓글 삭제 (로그인 필요, 본인 댓글만 삭제 가능, 하드 삭제)
     * @param actorUid 삭제자 UID
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     */
    public void deleteComment(String actorUid, String postId, String commentId) throws Exception {
        // 1. 댓글 존재 확인
        CollectionReference commentsRef = firestore.collection("posts").document(postId).collection("comments");
        DocumentReference ref = commentsRef.document(commentId);
        DocumentSnapshot existing = ref.get().get();
        
        if (!existing.exists()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다: " + commentId);
        }

        // 2. 본인 댓글만 삭제 가능
        String authorId = existing.getString("authorId");
        if (authorId == null || !authorId.equals(actorUid)) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED, "본인의 댓글만 삭제할 수 있습니다.");
        }

        // 3. Firestore 문서 삭제 (하드 삭제)
        ref.delete().get();
    }

    /**
     * 댓글 생성 요청 Validation
     */
    private void validateCreateRequest(CommentCreateRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글 내용은 필수입니다.");
        }
        String trimmed = request.body().trim();
        if (trimmed.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글은 최대 500자까지 입력 가능합니다.");
        }
    }

    /**
     * 댓글 수정 요청 Validation
     */
    private void validateUpdateRequest(CommentUpdateRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글 내용은 필수입니다.");
        }
        String trimmed = request.body().trim();
        if (trimmed.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "댓글은 최대 500자까지 입력 가능합니다.");
        }
    }

    /**
     * DocumentSnapshot을 CommentDto로 변환
     */
    private static CommentDto toDto(DocumentSnapshot doc, String postId) {
        return new CommentDto(
                doc.getId(),
                postId,
                doc.getString("body"),
                doc.getString("authorId"),
                doc.getString("authorName"),
                FirestoreUtils.toIsoString(doc.getTimestamp("createdAt")),
                FirestoreUtils.toIsoString(doc.getTimestamp("updatedAt"))
        );
    }

    /**
     * CommentDto를 CommentListItemDTO로 변환
     */
    private static CommentListItemDTO convertToListItem(CommentDto commentDto) {
        return new CommentListItemDTO(
                commentDto.commentId(),
                commentDto.postId(),
                commentDto.body(),
                commentDto.authorId(),
                commentDto.authorName(),
                commentDto.createdAt(),
                commentDto.updatedAt()
        );
    }

    /**
     * pageSize 검증
     */
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

