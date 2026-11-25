package com.example.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.example.chatapp.common.security.RestAccessDeniedHandler;
import com.example.chatapp.common.security.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception{ http.csrf(AbstractHttpConfigurer :: disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                            .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
                            .authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.POST, "/api/v1/auth/register","/api/v1/auth/login"
                            ,"/api/v1/auth/otp/verify","/api/v1/auth/otp/resend","/api/v1/auth/password/forgot","/api/v1/auth/password/reset"
                            ,"/api/v1/auth/refresh","/api/v1/auth/logout").permitAll().anyRequest().authenticated());
        return http.build();
    }
}
