package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiRouteErrorCode implements BaseErrorCode {
    AI_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch AI"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tối ưu tuyến đang bảo trì"),
    AI_NO_ORDERS_READY(HttpStatus.BAD_REQUEST, "Không có đơn hàng sẵn sàng để tối ưu tuyến"),
    AI_NO_AVAILABLE_SHIPPERS(HttpStatus.BAD_REQUEST, "Không có shipper khả dụng để thực hiện tối ưu"),
    AI_INVALID_PLAN_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái kế hoạch không hợp lệ cho thao tác này"),
    AI_ORDER_ASSIGNED_TO_OTHER(HttpStatus.CONFLICT, "Một số đơn hàng đã được gán cho shipper khác"),
    AI_SHIPPER_NOT_FOUND(HttpStatus.NOT_FOUND, "Shipper không tồn tại hoặc không hợp lệ"),
    AI_MANAGER_RESOLVE_FAILED(HttpStatus.FORBIDDEN, "Không tìm thấy nhân viên quản lý"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
