package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentSubmissionErrorCode implements BaseErrorCode {
    PAYMENT_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Đối soát không tồn tại"),
    PAYMENT_SUBMISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    PAYMENT_SUBMISSION_INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "Trạng thái yêu cầu chuyển không hợp lệ"),
    PAYMENT_SUBMISSION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái yêu cầu không hợp lệ"),
    PAYMENT_SUBMISSION_INVALID_NOTE(HttpStatus.BAD_REQUEST, "Ghi chú đối soát không được vượt quá 255 ký tự"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
