package com.logistics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

  // System
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
  INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "Invalid enum value"),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
  MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "Thiếu thông tin bắt buộc: %s"),
  EXPORT_EXCEL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra trong quá trình xuất file Excel"),

  // Cloudinary
  CLOUDINARY_FOLDER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary folder not found for key: %s"),
  CLOUDINARY_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary upload failed"),
  CLOUDINARY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary delete failed"),
  ;

  private final HttpStatus httpStatus;
  private final String message;

  public String getCode() {
    return this.name();
  }
}
