package com.example.chatapp.common.security;

@FunctionalInterface 
public interface AccessTokenVerifier {
    CurrentUser verify(String accessToken);
}