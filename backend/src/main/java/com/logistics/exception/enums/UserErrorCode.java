package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"),

    USER_EMPLOYEE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên nhân viên này"),
    USER_EMPLOYEE_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin phân quyền"),
    USER_EMPLOYEE_ALREADY_IN_ANOTHER_SHOP(HttpStatus.BAD_REQUEST, "Tài khoản này đã thuộc một cửa hàng khác"),
    USER_EMPLOYEE_PHONE_NUMBER_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng"),
    USER_EMPLOYEE_HAS_ACTIVE_ROLE(HttpStatus.BAD_REQUEST, "Nhân viên này đang có quyền khác, vui lòng thu hồi trước khi gán quyền mới"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
