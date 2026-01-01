package com.hutech.bookstore.exception;

public class AppException extends RuntimeException {
    private final Integer statusCode;

    public AppException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public AppException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}

