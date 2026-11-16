package com.example.chatapp.common.exception;

import com.example.chatapp.common.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception) {
        ApiError body = ApiError.of(
                exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
