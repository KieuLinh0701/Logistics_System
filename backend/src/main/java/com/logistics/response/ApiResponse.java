package com.logistics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
//    private LocalDateTime timestamp;
//
//    public static <T> ApiResponse<T> success(String message, T data) {
//        return new ApiResponse<T>(true, message, data, LocalDateTime.now());
//    }
//
//    public static <T> ApiResponse<T> success(T data) {
//        return new ApiResponse<T>(true, "Success", data, LocalDateTime.now());
//    }
//
//    public static <T> ApiResponse<T> failure(String message) {
//        return new ApiResponse<T>(false, message, null, LocalDateTime.now());
//    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, "Success", data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<T>(false, message, null);
    }
}