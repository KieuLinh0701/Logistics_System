package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementBatchErrorCode implements BaseErrorCode {
    SETTLEMENT_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phiên đối soát của bạn"),
    SETTLEMENT_BATCH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),

    SETTLEMENT_NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
    SETTLEMENT_MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
