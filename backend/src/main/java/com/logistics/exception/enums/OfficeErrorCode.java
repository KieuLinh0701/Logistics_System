package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OfficeErrorCode implements BaseErrorCode {
    OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Bưu cục không tồn tại"),
    OFFICE_HEAD_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tim thấy bưu cục chính"),
    OFFICE_CODE_EXISTED(HttpStatus.BAD_REQUEST, "Mã bưu cục đã tồn tại"),
    OFFICE_PHONE_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng"),
    OFFICE_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "Thông tin địa chỉ không được để trống"),
    OFFICE_COORDINATES_REQUIRED(HttpStatus.BAD_REQUEST, "Vĩ độ và kinh độ không được để trống"),
    OFFICE_INVALID_TYPE_OR_STATUS(HttpStatus.BAD_REQUEST, "Giá trị type/status không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
