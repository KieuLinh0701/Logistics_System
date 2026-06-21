package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SupportMessageErrorCode implements BaseErrorCode {
    SUPPORT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "Tin nhắn không được để trống"),
    SUPPORT_SEND_MESSAGE_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền gửi tin nhắn trong ticket này"),
    SUPPORT_VIEW_MESSAGES_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền xem tin nhắn của ticket này"),
    SUPPORT_INTERNAL_NOTE_DENIED(HttpStatus.FORBIDDEN, "Chỉ Manager/Admin mới được gửi ghi chú nội bộ"),
    SUPPORT_BOT_PREVIEW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo preview tin nhắn bot"),
    SUPPORT_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận file ảnh định dạng JPEG, PNG hoặc WebP"),
    SUPPORT_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "Kích thước ảnh vượt quá giới hạn 5MB"),
    SUPPORT_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Upload ảnh thất bại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
