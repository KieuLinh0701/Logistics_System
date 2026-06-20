package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OtpErrorCode implements BaseErrorCode {
    OTP_NOT_FOUND(HttpStatus.NOT_FOUND, "Mã OTP không tồn tại"),
    OTP_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ hoặc đã hết hạn");
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
