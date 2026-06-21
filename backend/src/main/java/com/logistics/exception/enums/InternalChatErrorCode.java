package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InternalChatErrorCode implements BaseErrorCode {
    INTERNAL_CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Phòng chat không tồn tại"),
    INTERNAL_CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập phòng chat này"),
    INTERNAL_CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "Tin nhắn không được để trống"),
    INTERNAL_CHAT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin người dùng"),
    INTERNAL_CHAT_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy quản lý cho bưu cục"),
    INTERNAL_CHAT_MANAGER_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin quản lý"),
    INTERNAL_CHAT_INVALID_ROLE(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    INTERNAL_CHAT_INVALID_PRINCIPAL(HttpStatus.BAD_REQUEST, "Thông tin xác thực không hợp lệ"),
    INTERNAL_CHAT_INVALID_SENDER(HttpStatus.BAD_REQUEST, "Người gửi không hợp lệ"),
    INTERNAL_CHAT_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận file ảnh định dạng JPEG, PNG hoặc WebP"),
    INTERNAL_CHAT_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "Kích thước ảnh vượt quá giới hạn 5MB"),
    INTERNAL_CHAT_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Upload ảnh thất bại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
