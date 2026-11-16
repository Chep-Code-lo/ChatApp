package com.example.chatapp.common.exception;

import com.example.chatapp.common.dto.ApiError;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodArgumentBuilder;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception) {
        ApiError body = ApiError.of(
                exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
    @ExceptionHandler(MethodArgumentNotVaildException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception){
        ApiError body = ApiError.of("VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ");
        return ResponseEntity.status(HttpStatusCode.BAD_REQUEST).body(body);
    }
}
