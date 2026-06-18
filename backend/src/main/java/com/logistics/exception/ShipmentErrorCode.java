package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShipmentErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến hàng"),
    NOT_PENDING(HttpStatus.BAD_REQUEST, "Chuyến hàng không ở trạng thái PENDING"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái chuyến hàng không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
