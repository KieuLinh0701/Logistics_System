package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeeConfigurationErrorCode implements BaseErrorCode {
    FEE_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình phí"),
    FEE_CONFIG_FEE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "Loại phí không được để trống"),
    FEE_CONFIG_CALCULATION_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "Loại tính phí không được để trống"),
    FEE_CONFIG_FEE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "Giá trị phí không được để trống"),
    FEE_CONFIG_INVALID_ENUM(HttpStatus.BAD_REQUEST, "Giá trị enum không hợp lệ: %s"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
