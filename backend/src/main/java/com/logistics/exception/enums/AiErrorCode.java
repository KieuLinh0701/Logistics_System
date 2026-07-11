package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements BaseErrorCode {
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable"),
    AI_SERVICE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI service request timed out"),
    AI_REQUEST_REJECTED(HttpStatus.BAD_GATEWAY, "AI service rejected the request"),
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI service returned an invalid response"),
    AI_OPTIMIZATION_FAILED(HttpStatus.BAD_GATEWAY, "AI optimization failed"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}