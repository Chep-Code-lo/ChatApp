package com.example.chatapp.common.security;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class AccessTokenFilter extends OncePerRequestFilter {
    private final AccessTokenVerifier accessTokenVerifier;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override 
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException{
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(authorization == null){
            filterChain.doFilter(request, response);
            return; 
        }
        CurrentUser user;
        try{
            if(!authorization.regionMatches(true, 0, "Bearer ", 0, 7)){
                throw new BadCredentialsException("Sai định dạng Bearer token");
            }
            String token = authorization.substring(7).trim();
            if(token.isEmpty()){
                throw new BadCredentialsException("Access token trống");
            }
            user = Objects.requireNonNull(
                accessTokenVerifier.verify(token), "AccessTokenFilter không được trả về null"
            );
        } catch(AuthenticationException exception){
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, exception);
            return ;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }
    
}
