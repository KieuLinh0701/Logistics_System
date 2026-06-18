package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"),
    IS_DEFAULT(HttpStatus.BAD_REQUEST, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa"),
    MAX_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Chỉ được tạo tối đa 10 địa chỉ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
