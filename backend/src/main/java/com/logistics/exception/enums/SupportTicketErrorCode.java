package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SupportTicketErrorCode implements BaseErrorCode {
    SUPPORT_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Ticket hỗ trợ không tồn tại"),
    SUPPORT_TICKET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập ticket này"),
    SUPPORT_TICKET_CLOSED(HttpStatus.BAD_REQUEST, "Ticket đã đóng, không thể thực hiện thao tác"),
    SUPPORT_TICKET_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Ticket đã đóng rồi"),
    SUPPORT_CREATE_TICKET_DENIED(HttpStatus.BAD_REQUEST, "Chỉ User mới được tạo ticket hỗ trợ"),
    SUPPORT_ASSIGN_DENIED(HttpStatus.FORBIDDEN, "Chỉ Admin mới có quyền phân công ticket"),
    SUPPORT_CLOSE_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh dấu giải quyết ticket này"),
    SUPPORT_FORCE_CLOSE_DENIED(HttpStatus.FORBIDDEN, "Chỉ Admin mới có quyền đóng hẳn ticket"),
    SUPPORT_REOPEN_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền mở lại ticket này"),
    SUPPORT_REOPEN_INVALID_STATUS(HttpStatus.BAD_REQUEST, "Chỉ có thể mở lại ticket đã đóng hoặc đã giải quyết"),
    SUPPORT_TICKET_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ticketId là bắt buộc"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
