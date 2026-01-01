package com.club.site.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 🚨 가장 중요한 부분: 아래 두 줄이 꼭 있어야 합니다.
                        // swagger-ui/** 는 화면(HTML)을 보여주는 경로
                        // /v3/api-docs/** 는 실제 데이터(JSON)를 가져오는 경로 (이게 막히면 저 에러가 뜹니다)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()

                        // API 경로 허용
                        .requestMatchers("/api/v1/**").permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}