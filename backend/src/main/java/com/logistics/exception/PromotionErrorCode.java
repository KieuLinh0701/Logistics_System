package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Khuyến mãi không tồn tại"),
    NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "Bạn không đủ điều kiện để dùng mã giảm giá"),
    EXPIRED(HttpStatus.BAD_REQUEST, "Khuyến mãi bạn chọn có thể đã thay đổi, hết hạn hoặc hết lượt sử dụng."),
    INVALID(HttpStatus.NOT_FOUND, "Khuyến mãi không hợp lệ"),
    CODE_EXISTED(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã tồn tại"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu"),
    CODE_REQUIRED(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không được để trống"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
