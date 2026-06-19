package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShipperAssignmentErrorCode implements BaseErrorCode {
    SHIPPER_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Phân công không tồn tại"),
    SHIPPER_ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "Nhân viên giao hàng đã được phân công khu vực này trong khoảng thời gian đã chọn!"),
    SHIPPER_ASSIGNMENT_CLOSED(HttpStatus.FORBIDDEN, "Phân công đã kết thúc, không thể chỉnh sửa!"),
    SHIPPER_ASSIGNMENT_END_TIME_INVALID(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải hơn thời điểm hiện tại!"),
    SHIPPER_ASSIGNMENT_START_TIME_INVALID(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải hơn thời điểm hiện tại!"),
    SHIPPER_ASSIGNMENT_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu!"),
    SHIPPER_ASSIGNMENT_CANNOT_DELETED(HttpStatus.BAD_REQUEST, "Không có quyền xóa phân công này!"),
    SHIPPER_ASSIGNMENT_DELETION_FORBIDDEN(HttpStatus.FORBIDDEN, "Phân công đã bắt đầu hoặc đã kết thúc, không thể xóa!"),
    SHIPPER_ASSIGNMENT_ID_INVALID(HttpStatus.BAD_REQUEST, "Mã nhân viên không hợp lệ!"),
    SHIPPER_ASSIGNMENT_WARD_CODE_INVALID(HttpStatus.BAD_REQUEST, "Phường/xã không hợp lệ!"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
