package com.club.site.config;

import com.club.site.auth.jwt.FirebaseTokenFilter;
import com.club.site.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            FirebaseAuth firebaseAuth
    ) throws Exception {

        http
                // 1. CORS 설정 가장 먼저 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 2. Preflight 요청(OPTIONS)은 무조건 허용 (CORS 처리를 위해)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. Static 리소스 허용 (테스트 페이지 등)
                        .requestMatchers("/token.html").permitAll()
                        .requestMatchers("/**/*.html", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.gif", "/**/*.ico", "/**/*.svg").permitAll()
                        
                        // 4. Swagger(SpringDoc) 관련 경로 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 5. Public API (로그인 없이 접근 가능)
                        // Members API
                        .requestMatchers(HttpMethod.GET, "/api/v1/members", "/api/v1/members/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/members/init").permitAll()
                        
                        // Posts API (Public Read)
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/**").permitAll()
                        
                        // Comments API (Public Read)
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**/comments").permitAll()
                        
                        // Skills API (Public Read)
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills").permitAll()
                        
                        // Health Check
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()

                        // 6. 나머지 API는 인증 필요
                        .anyRequest().authenticated()
                )

                // 5. Firebase 필터 추가
                .addFilterBefore(new FirebaseTokenFilter(firebaseAuth), UsernamePasswordAuthenticationFilter.class)

                // 6. 에러 핸들링 (인증 실패 시 JSON 응답)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), ApiResponse.fail("UNAUTHORIZED", "로그인이 필요합니다."));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), ApiResponse.fail("FORBIDDEN", "권한이 없습니다."));
                        })
                );

        return http.build();
    }

    // 🔥 [핵심] CORS 설정 (개발 환경에 최적화)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 오리진 (패턴으로 지정하여 127.0.0.1과 localhost 모두 허용)
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:*" // 포트가 바뀌어도 허용
        ));

        // 2. 허용할 메서드 (모두 허용)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 허용할 헤더 (모두 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 자격 증명 허용 (Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // 5. 브라우저가 preflight 결과를 캐싱하는 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}