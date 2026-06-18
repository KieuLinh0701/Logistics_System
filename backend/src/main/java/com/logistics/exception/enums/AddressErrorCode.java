package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements BaseErrorCode {
    ADDRESS_MAX_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Chỉ được tạo tối đa 10 địa chỉ"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"),
    ADDRESS_IS_DEFAULT(HttpStatus.BAD_REQUEST, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa"),
    ADDRESS_ALREADY_EXISTS(HttpStatus.CONFLICT, "Địa chỉ này đã tồn tại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
