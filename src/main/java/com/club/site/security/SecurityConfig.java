package com.club.site.security;

import com.club.site.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {

        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/_debug/**").permitAll()
                        .requestMatchers("/api/v1/hello").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/skills").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/skills/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/posts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/uploads/presign").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new FirebaseTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.fail("UNAUTHORIZED", "Authentication required")
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.fail("FORBIDDEN", "Forbidden")
                            );
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

