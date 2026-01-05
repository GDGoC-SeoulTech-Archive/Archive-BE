package com.club.site.web;

import com.google.api.gax.rpc.FailedPreconditionException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::formatFieldError)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("BAD_REQUEST", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(FailedPreconditionException.class)
    public ResponseEntity<ApiResponse<Void>> handleFirestoreIndexException(FailedPreconditionException e) {
        log.error("Firestore index required: {}", e.getMessage(), e);
        String message = e.getMessage();
        if (message != null && message.contains("index")) {
            // 인덱스 생성 링크 추출
            String indexLink = extractIndexLink(message);
            if (indexLink != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("FIRESTORE_INDEX_REQUIRED", 
                                "Firestore 인덱스가 필요합니다. 다음 링크에서 인덱스를 생성해주세요: " + indexLink));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("FIRESTORE_INDEX_REQUIRED", 
                        "Firestore 인덱스가 필요합니다. Firebase Console에서 인덱스를 생성해주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("INTERNAL_ERROR", "Unexpected error"));
    }

    private String extractIndexLink(String message) {
        // 에러 메시지에서 인덱스 생성 링크 추출
        if (message.contains("https://console.firebase.google.com")) {
            int start = message.indexOf("https://console.firebase.google.com");
            int end = message.indexOf("\n", start);
            if (end == -1) {
                end = message.length();
            }
            return message.substring(start, end).trim();
        }
        return null;
    }

    private static String formatFieldError(FieldError error) {
        if (error.getDefaultMessage() == null) {
            return error.getField() + " is invalid";
        }
        return error.getField() + ": " + error.getDefaultMessage();
    }
}


