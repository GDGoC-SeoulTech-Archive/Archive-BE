package com.club.site.auth.jwt;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 토큰 꺼내기
        String header = request.getHeader("Authorization");

        // 2. 토큰이 없거나 형식이 안 맞으면 그냥 통과 (SecurityConfig에서 막을 예정)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7); // "Bearer " 제거

        try {
            // 3. 파이어베이스에 검증 요청
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

            // 4. 검증 성공 시, Spring Security에 "이 사람 인증됨!" 도장 찍기
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(decodedToken.getUid(), null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 토큰 위조, 만료 등 실패 시 -> 401 에러를 주거나 그냥 통과시켜서 뒤에서 막게 함
            // 여기서는 로그만 찍고 통과시킴 (Security가 알아서 거름)
            System.out.println("토큰 검증 실패: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}