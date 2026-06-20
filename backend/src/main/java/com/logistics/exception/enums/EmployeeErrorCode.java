package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {
    EMPLOYEE_MANAGER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn chỉ có thể chỉnh sửa nhân viên trong bưu cục của mình."),
    EMPLOYEE_MANAGER_NOT_FOUND(HttpStatus.FORBIDDEN, "Bạn không phải quản lý bưu cục hoặc đã nghỉ"),
    EMPLOYEE_PHONE_NUMBER_EXISTED(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng"),
    EMPLOYEE_ROLE_INVALID(HttpStatus.BAD_REQUEST, "Chức vụ bạn chọn không hợp lệ"),
    EMPLOYEE_ACCOUNT_IN_OTHER_OFFICE(HttpStatus.CONFLICT, "Tài khoản này đang làm việc tại bưu cục khác"),
    EMPLOYEE_ACCOUNT_ALREADY_IN_OFFICE(HttpStatus.CONFLICT, "Tài khoản này đã là nhân viên ở bưu cục hiện tại"),
    EMPLOYEE_SHIFT_INVALID(HttpStatus.BAD_REQUEST, "Ca làm việc không hợp lệ"),
    EMPLOYEE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "Trạng thái làm việc không hợp lệ"),
    EMPLOYEE_CANNOT_CHANGE_ROLE_AND_LEAVE_SIMULTANEOUSLY(HttpStatus.BAD_REQUEST, "Không thể thay đổi chức vụ và cho nghỉ cùng lúc"),
    EMPLOYEE_LIMIT_ACTIVE_ROLES_REACHED(HttpStatus.BAD_REQUEST, "Đã đạt giới hạn chức vụ đang hoạt động"),
    EMPLOYEE_ACCOUNT_IN_OFFICE(HttpStatus.CONFLICT, "Tài khoản này đã là nhân viên ở bưu cục hiện tại"),
    EMPLOYEE_USER_OFFICE_MISSING(HttpStatus.BAD_REQUEST, "Không xác định được bưu cục của nhân viên!"),
    EMPLOYEE_OFFICE_MISMATCH(HttpStatus.FORBIDDEN, "Nhân viên không thuộc bưu cục của bạn!"),
    EMPLOYEE_SHIPPER_INVALID(HttpStatus.BAD_REQUEST, "Nhân viên không phải là nhân viên giao hàng!"),
    EMPLOYEE_DRIVER_INVALID(HttpStatus.BAD_REQUEST, "Nhân viên không phải là tài xế!"),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin nhân viên"),
    EMPLOYEE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên nhân viên này"),
    EMPLOYEE_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin phân quyền"),
    EMPLOYEE_ALREADY_IN_ANOTHER_SHOP(HttpStatus.BAD_REQUEST, "Tài khoản này đã thuộc một cửa hàng khác"),
    EMPLOYEE_HAS_ACTIVE_ROLE(HttpStatus.BAD_REQUEST, "Nhân viên này đang có quyền khác, vui lòng thu hồi trước khi gán quyền mới"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
