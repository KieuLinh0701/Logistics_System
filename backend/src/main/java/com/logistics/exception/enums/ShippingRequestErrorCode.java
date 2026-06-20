package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShippingRequestErrorCode implements BaseErrorCode {
    SHIPPING_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hỗ trợ"),
    SHIPPING_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền trên yêu ầu hỗ trợ này"),
    SHIPPING_REQUEST_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ với trạng thái đơn hàng hiện tại"),
    SHIPPING_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "Đã có yêu cầu tương tự cho đơn hàng này đang được xử lý"),
    SHIPPING_REQUEST_INVALID_RESPONSE(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được vượt quá 1000 ký tự"),
    SHIPPING_REQUEST_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Nội dung yêu cầu không được vượt quá 1000 ký tự"),
    SHIPPING_REQUEST_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Yêu cầu không thể hủy ở trạng thái hiện tại"),
    SHIPPING_REQUEST_EDIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Không thể thay đổi loại yêu cầu hoặc đơn hàng khi chỉnh sửa"),

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
