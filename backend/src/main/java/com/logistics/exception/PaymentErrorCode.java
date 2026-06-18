package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Nộp tiền không tồn tại"),
    BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Phiên đối soát không tồn tại"),
    BATCH_EXPORT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel phiên đối soát"),
    SUBMISSION_EXPORT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel nộp tiền"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    SETTLEMENT_NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
    SETTLEMENT_MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch: %s"),
    TRANSACTION_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, "Chữ ký không hợp lệ"),
    TRANSACTION_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "Không có giao dịch để xử lý"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
