package com.example.chatapp.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.example.chatapp.common.dto.ApiError;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class SecurityErrorWriter {
    private final JsonMapper JsonMapper;
    public void write(HttpServletResponse response, HttpStatus status, String code, String message)
    throws IOException{
        if(response.isCommitted()){
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError body = ApiError.of(code, message);
        response.getWriter().write(JsonMapper.writeValueAsString(body));
    }
}
