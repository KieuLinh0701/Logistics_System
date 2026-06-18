package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode implements BaseErrorCode {
    PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Khuyến mãi không tồn tại"),
    PROMOTION_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "Bạn không đủ điều kiện để dùng mã giảm giá"),
    PROMOTION_EXPIRED(HttpStatus.BAD_REQUEST, "Khuyến mãi bạn chọn có thể đã thay đổi, hết hạn hoặc hết lượt sử dụng."),
    PROMOTION_INVALID(HttpStatus.NOT_FOUND, "Khuyến mãi không hợp lệ"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
