package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OfficeErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Bưu cục không tồn tại"),
    CODE_EXISTED(HttpStatus.BAD_REQUEST, "Mã bưu cục đã tồn tại"),
    PHONE_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng"),
    ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "Thông tin địa chỉ không được để trống"),
    COORDINATES_REQUIRED(HttpStatus.BAD_REQUEST, "Vĩ độ và kinh độ không được để trống"),
    INVALID_TYPE_OR_STATUS(HttpStatus.BAD_REQUEST, "Giá trị type/status không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
