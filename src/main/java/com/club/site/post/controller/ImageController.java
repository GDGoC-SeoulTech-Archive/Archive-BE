package com.club.site.post.controller;

import com.club.site.post.dto.ImageInfo;
import com.club.site.web.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    // 로컬 자체에 이미지 저장
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public ApiResponse<List<ImageInfo>> uploadImages(
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<ImageInfo> uploadedImages = new ArrayList<>();

        // 폴더가 없으면 만들기
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                // 파일명 중복 방지 (UUID 사용)
                String originalFilename = file.getOriginalFilename();
                String uuid = UUID.randomUUID().toString();

                String extension = ".jpg";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String savedFileName = uuid + extension;
                Path destinationFile = Paths.get(UPLOAD_DIR + savedFileName);
                // 실제 저장
                file.transferTo(destinationFile);

                // (나중에 배포하면 도메인 주소로 바뀌어야 함)
                String fileUrl = "http://localhost:8080/images/" + savedFileName;

                uploadedImages.add(new ImageInfo(fileUrl, savedFileName));

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("이미지 저장 실패: " + e.getMessage());
            }
        }

        return ApiResponse.ok(uploadedImages);
    }
}