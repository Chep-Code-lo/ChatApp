package com.example.chatapp.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class RestAccessDeniedHandler implements AccessDeniedHandler{
    private final SecurityErrorWriter errorWriter;
    @Override 
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
    throws IOException{
        errorWriter.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
    }
    
}
