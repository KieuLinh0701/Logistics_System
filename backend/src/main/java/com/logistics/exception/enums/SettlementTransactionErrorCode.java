package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementTransactionErrorCode implements BaseErrorCode {
    SETTLEMENT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch: %s"),
    SETTLEMENT_TRANSACTION_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, "Chữ ký không hợp lệ"),
    SETTLEMENT_TRANSACTION_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "Không có giao dịch để xử lý"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
