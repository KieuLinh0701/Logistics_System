package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IncidentReportErrorCode implements BaseErrorCode {
    INCIDENT_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo sự cố"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
