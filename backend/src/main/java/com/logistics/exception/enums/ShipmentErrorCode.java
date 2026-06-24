package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShipmentErrorCode implements BaseErrorCode {
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Chuyến hàng không tồn tại hoặc không có quyền"),
    SHIPMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thưc hiện thao tác này"),
    SHIPMENT_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Chuyến hàng đã thực hiện, không thể hủy!"),
    SHIPMENT_INVALID_TYPE(HttpStatus.BAD_REQUEST, "Loại chuyến hàng không hợp lệ"),
    SHIPMENT_INVALID_DELIVERY_TYPE(HttpStatus.BAD_REQUEST, "Loại chuyến là chuyến giao hàng không cần chọn xe!"),
    SHIPMENT_INVALID_TRANSFER_TYPE(HttpStatus.BAD_REQUEST, "Loại chuyến là chuyến giao hàng không cần chọn xe!"),
    SHIPMENT_DELIVERY_INVALID_OFFICE_DESTINATION(HttpStatus.BAD_REQUEST, "Loại chuyến là chuyến giao hàng không cần chọn bưu cục đến!"),
    SHIPMENT_INVALID_OFFICE_DESTINATION(HttpStatus.BAD_REQUEST, "Bưu cục đến không được trùng với bưu cục xuất phát!"),
    SHIPMENT_ORDER_OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST,"Một số đơn không nằm trong khu vực phân công của nhân viên giao hàng."),
    SHIPMENT_NOT_PENDING(HttpStatus.BAD_REQUEST, "Chuyến hàng không ở trạng thái PENDING"),
    SHIPMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái chuyến hàng không hợp lệ"),
    SHIPMENT_CANNOT_ADD_ORDERS(HttpStatus.BAD_REQUEST, "Chuyến hàng không thể thêm đơn hàng!"),
    SHIPMENT_CANNOT_DELETE_ORDERS(HttpStatus.BAD_REQUEST, "Chuyến hàng không thể xóa các đơn hàng!"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
