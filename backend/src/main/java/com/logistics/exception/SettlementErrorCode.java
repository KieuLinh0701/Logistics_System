package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements BaseErrorCode {
    NO_COD(HttpStatus.BAD_REQUEST, "Đơn hàng không có COD"),
    NO_OPEN_BATCH(HttpStatus.BAD_REQUEST, "Không có phiên đối soát nào đang mở"),
    NO_SUBMISSION(HttpStatus.BAD_REQUEST, "Không tìm thấy bản ghi nộp tiền"),
    AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "Số tiền vượt quá số dư cần đối soát"),
    NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
    MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),
    ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
