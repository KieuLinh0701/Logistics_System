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
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
