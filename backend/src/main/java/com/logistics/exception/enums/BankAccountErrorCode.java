package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BankAccountErrorCode implements BaseErrorCode {
    BANK_ACCOUNT_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Chỉ được tạo tối đa 5 tài khoản ngân hàng"),
    BANK_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản ngân hàng"),
    BANK_ACCOUNT_IS_DEFAULT(HttpStatus.BAD_REQUEST, "Vui lòng chọn tài khoản mặc định khác trước khi xóa"),
    BANK_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "Bạn cần thêm tài khoản ngân hàng trong hồ sơ cá nhân để nhận tiền COD hoặc thanh toán khi tạo đơn hàng."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
