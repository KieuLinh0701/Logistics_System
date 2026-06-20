package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements BaseErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"),
    ACCOUNT_LOCKED_DUE_TO_OVERDUE(HttpStatus.BAD_REQUEST, "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới."),
    ACCOUNT_OLD_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST,  "Mật khẩu cũ không chính xác"),
    ACCOUNT_PASSWORD_INCORRECT(HttpStatus.UNAUTHORIZED, "Mật khẩu không chính xác"),
    ACCOUNT_NEW_EMAIL_DUPLICATE_CURRENT(HttpStatus.BAD_REQUEST, "Email mới không được trùng với email hiện tại"),
    ACCOUNT_EMAIL_ALREADY_IN_USE(HttpStatus.CONFLICT, "Email này đã được sử dụng bởi tài khoản khác"),
    ACCOUNT_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa"),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Tài khoản chưa được xác thực"),
    ACCOUNT_NO_VALID_ROLE(HttpStatus.FORBIDDEN, "Tài khoản không có role nào hợp lệ"),
    ACCOUNT_PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự"),
    ACCOUNT_PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"),
    ACCOUNT_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "Người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn");
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
