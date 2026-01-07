package com.club.site.comment.controller;

import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.comment.dto.CommentCreateRequest;
import com.club.site.comment.dto.CommentCreateResponse;
import com.club.site.comment.dto.CommentDeleteResponse;
import com.club.site.comment.dto.CommentDetail;
import com.club.site.comment.dto.CommentDto;
import com.club.site.comment.dto.CommentListResponse;
import com.club.site.comment.dto.CommentUpdateRequest;
import com.club.site.comment.dto.CommentUpdateResponse;
import com.club.site.comment.service.CommentService;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 댓글 리스트 조회 (Public)
     * GET /api/v1/posts/{postId}/comments?pageSize=50&cursor=...
     */
    @GetMapping
    public ApiResponse<CommentListResponse> list(
            @PathVariable String postId,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        CommentListResponse response = commentService.listComments(postId, pageSize, cursor);
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 생성 (로그인 필요)
     * POST /api/v1/posts/{postId}/comments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentCreateResponse>> create(
            Authentication authentication,
            @PathVariable String postId,
            @Valid @RequestBody CommentCreateRequest request
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        CommentDto commentDto = commentService.createComment(principal.uid(), postId, request);
        CommentDetail commentDetail = CommentDetail.from(commentDto);
        CommentCreateResponse response = new CommentCreateResponse(commentDto.commentId(), commentDetail);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * 댓글 수정 (로그인 필요, 본인 댓글만)
     * PUT /api/v1/posts/{postId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ApiResponse<CommentUpdateResponse> update(
            Authentication authentication,
            @PathVariable String postId,
            @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        CommentDto commentDto = commentService.updateComment(principal.uid(), postId, commentId, request);
        CommentDetail commentDetail = CommentDetail.from(commentDto);
        CommentUpdateResponse response = new CommentUpdateResponse(commentId, commentDetail);
        
        return ApiResponse.ok(response);
    }

    /**
     * 댓글 삭제 (로그인 필요, 본인 댓글만, 하드 삭제)
     * DELETE /api/v1/posts/{postId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ApiResponse<CommentDeleteResponse> delete(
            Authentication authentication,
            @PathVariable String postId,
            @PathVariable String commentId
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        commentService.deleteComment(principal.uid(), postId, commentId);
        return ApiResponse.ok(new CommentDeleteResponse(true));
    }
}

