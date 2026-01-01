package com.hutech.bookstore.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer statusCode;
    private T data;
    private String message;
    private LocalDateTime timestamp;

    public ApiResponse(Integer statusCode, T data, String message) {
        this.statusCode = statusCode;
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "Success");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(201, data, message);
    }

    public static <T> ApiResponse<T> error(Integer statusCode, String message) {
        return new ApiResponse<>(statusCode, null, message);
    }
}

