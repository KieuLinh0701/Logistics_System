package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IncidentErrorCode implements BaseErrorCode {
    INCIDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Sự cố không tồn tại"),
    INCIDENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền trên báo cáo này"),
    INCIDENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trái thái yêu cầu không hợp lệ"),
    INCIDENT_INVALID_RESPONSE(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được vượt quá 1000 ký tự"),
    INCIDENT_INVALID_TRANSFER_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái yêu cầu chuyển không hợp lệ")

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
