package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IncidentErrorCode implements BaseErrorCode {
    INCIDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Sự cố không tồn tại"),
    INCIDENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền trên báo cáo này");    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
