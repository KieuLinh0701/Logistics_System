package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SupportErrorCode implements BaseErrorCode {
    SUPPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    SUPPORT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập chức năng này"),
    SUPPORT_BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
