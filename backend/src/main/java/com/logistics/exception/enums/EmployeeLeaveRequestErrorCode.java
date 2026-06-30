package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmployeeLeaveRequestErrorCode implements BaseErrorCode {
    EMPLOYEE_LEAVE_REQUEST_MISSING_OFFICE(HttpStatus.BAD_REQUEST, "Nhân viên chưa được gán bưu cục"),
    EMPLOYEE_LEAVE_REQUEST_LEAVE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "Ngày nghỉ không được để trống"),
    EMPLOYEE_LEAVE_REQUEST_INVALID_LEAVE_DATE(HttpStatus.BAD_REQUEST, "Ngày nghỉ không được nhỏ hơn ngày hiện tại"),
    EMPLOYEE_LEAVE_REQUEST_MISSING_CUSTOM_REASON(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do riêng khi chọn OTHER"),
    EMPLOYEE_LEAVE_REQUEST_INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái duyệt chỉ được là APPROVED hoặc REJECTED"),
    EMPLOYEE_LEAVE_REQUEST_LEAVE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn xin nghỉ"),
    EMPLOYEE_LEAVE_REQUEST_INVALID_LEAVE_STATUS(HttpStatus.CONFLICT, "Chỉ có thể hủy hoặc duyệt đơn ở trạng thái chờ duyệt"),
    EMPLOYEE_LEAVE_REQUEST_UNAUTHORIZED_CANCEL(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy đơn nghỉ phép này"),
    EMPLOYEE_LEAVE_REQUEST_UNAUTHORIZED_APPROVE(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt đơn nghỉ phép của bưu cục khác"),
    EMPLOYEE_LEAVE_REQUEST_ROLE_REQUIRED_DRIVER_OR_SHIPPER(HttpStatus.FORBIDDEN, "Chỉ tài xế hoặc shipper mới được thao tác đơn nghỉ phép"),
    EMPLOYEE_LEAVE_REQUEST_ROLE_REQUIRED_MANAGER(HttpStatus.FORBIDDEN, "Chỉ quản lý bưu cục mới được thao tác chức năng này"),
    EMPLOYEE_LEAVE_REQUEST_DUPLICATE(HttpStatus.CONFLICT, "Bạn đã có đơn xin nghỉ cho ngày và ca này"),
    ;
    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
