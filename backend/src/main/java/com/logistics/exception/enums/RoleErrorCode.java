package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoleErrorCode implements BaseErrorCode {
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Nhóm quyền không tồn tại"),
    ROLE_NAME_EXISTS(HttpStatus.CONFLICT, "Tên nhóm quyền đã tồn tại"),
    ROLE_IN_USE(HttpStatus.BAD_REQUEST, "Không thể xóa nhóm quyền đang được sử dụng"),
    ROLE_INVALID_PERMISSION_GROUP(HttpStatus.BAD_REQUEST, "Một số nhóm quyền không tồn tại hoặc không hợp lệ"),
    ROLE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên nhóm quyền này"),
    ROLE_INVALID_HIERARCHY_GROUP(HttpStatus.BAD_REQUEST, "Nhóm quyền %s yêu cầu phải chọn nhóm cha là %s trước"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
