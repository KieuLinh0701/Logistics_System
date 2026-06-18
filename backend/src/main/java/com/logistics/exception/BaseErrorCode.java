package com.logistics.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    
    String getMessage();
    String getCode();
}
