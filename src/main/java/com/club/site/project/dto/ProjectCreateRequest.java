package com.club.site.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String body,

        String startDate,

        @Size(max = 10, message = "이미지는 최대 10개까지 등록 가능합니다.")
        List<ImageInfo> images,

        List<String> skills,
        List<String> members
) {
}
