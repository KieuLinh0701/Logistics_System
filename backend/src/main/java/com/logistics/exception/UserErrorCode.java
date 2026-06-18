package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"),
    EMAIL_EXISTED(HttpStatus.BAD_REQUEST, "Email đã tồn tại"),
    PHONE_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy role id=%d"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
