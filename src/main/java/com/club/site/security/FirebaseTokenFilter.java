package com.club.site.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException; // 예외 처리 구체화
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    // [수정] static 호출 대신 주입받을 필드 선언
    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                // [수정] 주입받은 객체 사용
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

                Map<String, Object> claims = decodedToken.getClaims();
                String role = String.valueOf(claims.getOrDefault("role", "user"));

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                // [참고] Firebase Custom Claims에 "admin"이 설정되어 있어야 동작함
                if ("admin".equalsIgnoreCase(role)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                FirebasePrincipal.from(decodedToken),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (FirebaseAuthException e) {
                // 토큰 검증 실패 시 로그를 남기거나, 401 에러를 명시적으로 줄 수 있음
                // 현재는 그냥 넘어가서 SecurityConfig의 EntryPoint가 처리하게 둠 (나쁘지 않음)
            } catch (Exception e) {
                // 기타 에러 처리
            }
        }

        filterChain.doFilter(request, response);
    }
}