package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements BaseErrorCode {
    SETTLEMENT_NO_COD(HttpStatus.BAD_REQUEST, "Đơn hàng không có COD"),
    SETTLEMENT_NO_OPEN_BATCH(HttpStatus.BAD_REQUEST, "Không có phiên đối soát nào đang mở"),
    SETTLEMENT_NO_SUBMISSION(HttpStatus.BAD_REQUEST, "Không tìm thấy bản ghi nộp tiền"),
    SETTLEMENT_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "Số tiền vượt quá số dư cần đối soát"),
    SETTLEMENT_NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
    SETTLEMENT_MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),
    SETTLEMENT_ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
