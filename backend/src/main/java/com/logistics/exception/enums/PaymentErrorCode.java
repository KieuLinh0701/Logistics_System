package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {
    PAYMENT_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Nộp tiền không tồn tại"),
    PAYMENT_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Phiên đối soát không tồn tại"),
    PAYMENT_BATCH_EXPORT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel phiên đối soát"),
    PAYMENT_SUBMISSION_EXPORT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel nộp tiền"),
    PAYMENT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    PAYMENT_ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    PAYMENT_SETTLEMENT_NO_PENDING_DEBT(HttpStatus.BAD_REQUEST, "Không có khoản nợ nào cần thanh toán"),
    PAYMENT_SETTLEMENT_MIN_PAYMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Tổng nợ phải từ %s₫ trở lên"),
    PAYMENT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch: %s"),
    PAYMENT_TRANSACTION_INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, "Chữ ký không hợp lệ"),
    PAYMENT_TRANSACTION_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "Không có giao dịch để xử lý"),
    PAYMENT_SIGNATURE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi ký dữ liệu thanh toán"),
    PAYMENT_VNPAY_LINK_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo link thanh toán VNPay"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
