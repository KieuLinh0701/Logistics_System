package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseErrorCode {

    // System
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ"),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "Giá trị không nằm trong danh mục cho phép"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không được thực hiện"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "Thiếu thông tin bắt buộc: %s"),
    EXPORT_EXCEL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel"),
    PARSE_ATTACHMENTS_FAILED(HttpStatus.BAD_REQUEST, "Có lỗi xảy ra khi phân tích dữ liệu"),

    // Cloudinary
    CLOUDINARY_FOLDER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary folder not found for key: %s"),
    CLOUDINARY_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Upload ảnh thất bại"),
    CLOUDINARY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary delete failed"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
