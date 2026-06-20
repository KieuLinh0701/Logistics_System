package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeeErrorCode implements BaseErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"),
    ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    ACCOUNT_OLD_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST,  "Mật khẩu cũ không chính xác"),
    ACCOUNT_PASSWORD_INCORRECT(HttpStatus.UNAUTHORIZED, "Mật khẩu không chính xác"),
    ACCOUNT_NEW_EMAIL_DUPLICATE_CURRENT(HttpStatus.BAD_REQUEST, "Email mới không được trùng với email hiện tại"),
    ACCOUNT_EMAIL_ALREADY_IN_USE(HttpStatus.CONFLICT, "Email này đã được sử dụng bởi tài khoản khác"),
    ACCOUNT_OTP_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ hoặc đã hết hạn");
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
