package com.example.chatapp.common.dto;
public record ApiError (ErroDetail error) {
    public record ErroDetail(String code, String message){
        public static ApiError of(String code, String message){
            return new ApiError(new ErroDetail(code, message));
        }
    }
}
