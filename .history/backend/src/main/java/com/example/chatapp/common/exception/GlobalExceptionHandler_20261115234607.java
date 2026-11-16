package com.example.chatapp.common.exception;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception){
        ApiError body = ApiError.of(
            exception.getCode(), exception.getMessage()
        );
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
