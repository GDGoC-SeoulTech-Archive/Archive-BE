package com.club.site.common.exception;

import com.club.site.common.error.ErrorCode;
import com.club.site.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = getHttpStatus(errorCode);
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED, INVALID_TOKEN, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, SUPER_ADMIN_REQUIRED, ADMIN_REQUIRED, POST_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_INPUT, BAD_REQUEST, INVALID_GENERATION, INVALID_PART -> HttpStatus.BAD_REQUEST;
            case MEMBER_NOT_FOUND, POST_NOT_FOUND, SKILL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
