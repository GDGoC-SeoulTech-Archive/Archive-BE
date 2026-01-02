package com.club.site.news.controller;

import com.club.site.web.ApiResponse;
import com.club.site.news.dto.NewsDTO;
import com.club.site.news.service.NewsService;
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
        return ApiResponse.ok(newsService.saveMockNews());
    }

    // 조회 (데이터 가져오기)
    @GetMapping
    public ApiResponse<List<NewsDTO>> getAllNews() {
        return ApiResponse.ok(newsService.getAllNews());
    }
}