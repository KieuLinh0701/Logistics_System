package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSettlementScheduleErrorCode implements BaseErrorCode {
    SETTLEMENT_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Người dùng chưa có lịch đối soát"),
    SETTLEMENT_SCHEDULE_INVALID_DAY_COUNT(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất 1 ngày"),
    SETTLEMENT_SCHEDULE_INVALID_DAY_FORMAT(HttpStatus.BAD_REQUEST, "Ngày không hợp lệ");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}