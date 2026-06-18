package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ServiceTypeErrorCode implements BaseErrorCode {
    SERVICE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "Dịch vụ vận chuyển không tồn tại"),
    SERVICE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Mã dịch vụ không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
