package com.logistics.exception.enums;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();

    String getMessage();
}
