package com.club.site.common.controller;

import com.club.site.common.response.ApiResponse;
import com.club.site.common.dto.NewsDTO;
import com.club.site.common.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // React
public class NewsController {

    private final NewsService newsService;

    // 초기화 버튼 (데이터 저장)
    @PostMapping("/init")
    public ApiResponse<String> initNews() {
        return ApiResponse.success(newsService.saveMockNews());
    }

    // 조회 (데이터 가져오기)
    @GetMapping
    public ApiResponse<List<NewsDTO>> getAllNews() {
        return ApiResponse.success(newsService.getAllNews());
    }
}