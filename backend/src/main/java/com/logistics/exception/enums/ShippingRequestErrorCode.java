package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShippingRequestErrorCode implements BaseErrorCode {
    SHIPPING_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"),
    SHIPPING_REQUEST_INVALID_TYPE(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    SHIPPING_REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "Yêu cầu đã được xử lý"),
    SHIPPING_REQUEST_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Đơn hàng liên quan không tồn tại"),
    SHIPPING_REQUEST_SHIPPER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin shipper"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
