package com.logistics.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class RecruitmentException extends RuntimeException {
    private final HttpStatus status;

    public RecruitmentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
