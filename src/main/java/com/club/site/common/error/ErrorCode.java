package com.club.site.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 인증/인가
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다."),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다."),

    // 멤버
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "멤버를 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS("MEMBER_ALREADY_EXISTS", "이미 존재하는 멤버입니다."),
    INVALID_GENERATION("INVALID_GENERATION", "유효하지 않은 기수입니다."),
    INVALID_PART("INVALID_PART", "유효하지 않은 파트입니다."),

    // 스킬
    SKILL_NOT_FOUND("SKILL_NOT_FOUND", "스킬을 찾을 수 없습니다."),
    SKILL_ALREADY_EXISTS("SKILL_ALREADY_EXISTS", "이미 존재하는 스킬입니다."),
    SKILL_INACTIVE("SKILL_INACTIVE", "비활성화된 스킬입니다."),

    // 게시글
    POST_NOT_FOUND("POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED("POST_ACCESS_DENIED", "게시글에 접근할 수 없습니다."),

    // 관리자
    SUPER_ADMIN_REQUIRED("SUPER_ADMIN_REQUIRED", "슈퍼 관리자 권한이 필요합니다."),
    ADMIN_REQUIRED("ADMIN_REQUIRED", "관리자 권한이 필요합니다."),

    // 공통
    INVALID_INPUT("INVALID_INPUT", "잘못된 입력입니다."),
    INVALID_ARGUMENT("INVALID_ARGUMENT", "잘못된 인자입니다."),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다.");

    private final String code;
    private final String message;
}
