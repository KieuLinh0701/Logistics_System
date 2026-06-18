package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ServiceTypeErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Dịch vụ vận chuyển không tồn tại"),
    INVALID(HttpStatus.BAD_REQUEST, "Mã dịch vụ không hợp lệ"),
    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "Tên loại dịch vụ không được để trống"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
