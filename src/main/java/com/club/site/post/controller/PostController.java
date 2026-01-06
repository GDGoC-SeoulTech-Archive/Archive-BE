package com.club.site.post.controller;

import com.club.site.common.error.ErrorCode;
import com.club.site.common.exception.BusinessException;
import com.club.site.post.dto.PostCreateRequest;
import com.club.site.post.dto.PostCreateResponse;
import com.club.site.post.dto.PostDeleteResponse;
import com.club.site.post.dto.PostDto;
import com.club.site.post.dto.PostDetailResponse;
import com.club.site.post.dto.PostListResponse;
import com.club.site.post.dto.PostUpdateRequest;
import com.club.site.post.dto.PostUpdateResponse;
import com.club.site.post.service.PostService;
import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 게시글 리스트 조회 (Public)
     * GET /api/v1/posts?pageSize=20&cursor=...
     */
    @GetMapping
    public ApiResponse<PostListResponse> list(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        PostListResponse response = postService.listPosts(pageSize, cursor);
        return ApiResponse.ok(response);
    }

    /**
     * 게시글 상세 조회 (Public)
     * GET /api/v1/posts/{postId}
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> get(@PathVariable String postId) throws Exception {
        PostDto postDto = postService.getPost(postId);
        PostDetailResponse.PostDetail postDetail = PostDetailResponse.PostDetail.from(postDto);
        return ApiResponse.ok(new PostDetailResponse(postDetail));
    }

    /**
     * 게시글 생성 (로그인 필요)
     * POST /api/v1/posts
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> create(
            Authentication authentication,
            @Valid @RequestBody PostCreateRequest request
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        PostDto postDto = postService.createPost(principal.uid(), request);
        PostDetailResponse.PostDetail postDetail = PostDetailResponse.PostDetail.from(postDto);
        PostCreateResponse response = new PostCreateResponse(postDto.id(), postDetail);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * 게시글 수정 (로그인 필요)
     * PUT /api/v1/posts/{postId}
     */
    @PutMapping("/{postId}")
    public ApiResponse<PostUpdateResponse> update(
            Authentication authentication,
            @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequest request
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        PostDto postDto = postService.updatePost(principal.uid(), postId, request);
        PostDetailResponse.PostDetail postDetail = PostDetailResponse.PostDetail.from(postDto);
        PostUpdateResponse response = new PostUpdateResponse(postId, postDetail);
        
        return ApiResponse.ok(response);
    }

    /**
     * 게시글 삭제 (로그인 필요, 하드 삭제)
     * DELETE /api/v1/posts/{postId}
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<PostDeleteResponse> delete(
            Authentication authentication,
            @PathVariable String postId
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        
        postService.deletePost(principal.uid(), postId);
        return ApiResponse.ok(new PostDeleteResponse(true));
    }
}


