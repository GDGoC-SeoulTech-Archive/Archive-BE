package com.club.site.post.controller;

import com.club.site.common.dto.PagedResult;
import com.club.site.post.dto.PostDto;
import com.club.site.post.dto.PostResponse;
import com.club.site.post.dto.PostUpdateRequest;
import com.club.site.post.service.PostService;
import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ApiResponse<PagedResult<PostDto>> list(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        return ApiResponse.ok(postService.listPosts(pageSize, cursor));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> get(@PathVariable String postId) throws Exception {
        return ApiResponse.ok(new PostResponse(postService.getPost(postId)));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> update(
            Authentication authentication,
            @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequest request
    ) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        return ApiResponse.ok(new PostResponse(
                postService.updatePost(principal.uid(), postId, request.title(), request.body(), request.eventDate(), request.imageUrls())
        ));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String postId) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        postService.deletePost(principal.uid(), postId);
        return ApiResponse.ok();
    }
}


