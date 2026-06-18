package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShipmentErrorCode implements BaseErrorCode {
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến hàng"),
    SHIPMENT_NOT_PENDING(HttpStatus.BAD_REQUEST, "Chuyến hàng không ở trạng thái PENDING"),
    SHIPMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái chuyến hàng không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
