package com.club.site.upload.service;

import com.club.site.util.UrlUtils;
import com.club.site.web.ApiException;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UploadService {
    private final StorageClient storageClient;

    public UploadService(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    public PresignResult presign(String uid, String contentType, String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ApiException("BAD_REQUEST", "fileName must be a base name", HttpStatus.BAD_REQUEST);
        }
        String objectName = "uploads/" + uid + "/" + UUID.randomUUID() + "-" + fileName;

        try {
            if (storageClient.bucket() == null) {
                throw new ApiException("NOT_CONFIGURED", "Storage bucket is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            throw new ApiException("NOT_CONFIGURED", "Storage bucket is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String bucketName = storageClient.bucket().getName();
        Storage storage = storageClient.bucket().getStorage();

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(contentType)
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                Duration.ofMinutes(15).toSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );

        String publicUrl = "https://storage.googleapis.com/" + bucketName + "/" + objectName;
        UrlUtils.requireHttpUrl(publicUrl, "publicUrl");

        return new PresignResult(signedUrl.toString(), publicUrl);
    }

    public record PresignResult(String uploadUrl, String publicUrl) {
    }
}

