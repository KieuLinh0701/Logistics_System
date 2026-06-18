package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OfficeErrorCode implements BaseErrorCode {
    OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Bưu cục không tồn tại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
