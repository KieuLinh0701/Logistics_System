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
    SHIPMENT_NOT_ACTIVE_FOR_ORDER(HttpStatus.BAD_REQUEST, "Đơn hàng không thuộc chuyến DELIVERY đang hoạt động"),
    SHIPMENT_NOT_DELIVERY(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận chuyến DELIVERY cho thao tác này"),
    SHIPMENT_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "Chuyến hàng đã được bắt đầu"),
    SHIPMENT_NOT_STARTED(HttpStatus.BAD_REQUEST, "Chuyến hàng chưa được bắt đầu"),
    SHIPMENT_EMPTY(HttpStatus.BAD_REQUEST, "Chuyến hàng chưa có đơn nào, không thể bắt đầu"),
    SHIPMENT_HAS_ACTIVE_ORDERS(HttpStatus.BAD_REQUEST, "Chuyến hàng còn đơn chưa hoàn tất, không thể kết thúc"),
    SHIPMENT_ORDER_NOT_IN_SHIPMENT(HttpStatus.BAD_REQUEST, "Đơn hàng không thuộc chuyến này"),
    SHIPMENT_NOT_ASSIGNED(HttpStatus.FORBIDDEN, "Bạn không phải nhân viên được gán cho chuyến này"),
    SHIPMENT_ORDERS_NOT_SCANNED(HttpStatus.CONFLICT, "Không thể bắt đầu chuyến. Còn %d/%d đơn chưa xác nhận lên xe: %s"),
    SHIPMENT_ORDER_NOT_IN_SHIPMENT_OF_SHIPPER(HttpStatus.FORBIDDEN, "Đơn hàng không thuộc chuyến hàng của bạn"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
