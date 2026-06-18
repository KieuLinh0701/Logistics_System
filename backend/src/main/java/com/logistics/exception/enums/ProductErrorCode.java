package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements BaseErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"),
    PRODUCT_NOT_OWNED(HttpStatus.FORBIDDEN, "Sản phẩm %s không thuộc quyền sở hữu của bạn"),
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã ngưng bán"),
    PRODUCT_INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s vượt quá tồn kho (%d)"),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm %s đã hết hàng"),
    PRODUCT_HAS_ORDER(HttpStatus.BAD_REQUEST, "Sản phẩm đã có đơn hàng, không thể xóa"),
    PRODUCT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này"),
    PRODUCT_NAME_EXISTS(HttpStatus.CONFLICT, "Sản phẩm với tên này đã tồn tại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
