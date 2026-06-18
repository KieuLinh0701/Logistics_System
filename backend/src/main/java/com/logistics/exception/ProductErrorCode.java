package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements BaseErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"),
    NOT_OWNED(HttpStatus.FORBIDDEN, "Sản phẩm %s không thuộc quyền sở hữu của bạn"),
    INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã ngưng bán"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s vượt quá tồn kho (%d)"),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã hết hàng"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
