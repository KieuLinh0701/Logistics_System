package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeeConfigurationErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình phí"),
    FEE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "Loại phí không được để trống"),
    CALCULATION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "Loại tính phí không được để trống"),
    FEE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "Giá trị phí không được để trống"),
    INVALID_ENUM(HttpStatus.BAD_REQUEST, "Giá trị enum không hợp lệ: %s"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
