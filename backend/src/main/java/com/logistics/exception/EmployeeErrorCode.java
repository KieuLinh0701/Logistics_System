package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin nhân viên"),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên nhân viên này"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin phân quyền"),
    ALREADY_IN_ANOTHER_SHOP(HttpStatus.BAD_REQUEST, "Tài khoản này đã thuộc một cửa hàng khác"),
    PHONE_NUMBER_EXISTED(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng"),
    HAS_ACTIVE_ROLE(HttpStatus.BAD_REQUEST, "Nhân viên này đang có quyền khác, vui lòng thu hồi trước khi gán quyền mới"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
