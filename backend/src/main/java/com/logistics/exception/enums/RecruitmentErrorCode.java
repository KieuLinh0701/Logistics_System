package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements BaseErrorCode {
    // Job Posting Errors
    RECRUITMENT_JOB_POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"),
    RECRUITMENT_JOB_POSTING_CLOSED(HttpStatus.BAD_REQUEST, "Tin tuyển dụng đã đóng"),
    RECRUITMENT_JOB_POSTING_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "Số lượng cần tuyển phải lớn hơn 0"),
    RECRUITMENT_JOB_POSTING_SHIFT_REQUIRED(HttpStatus.BAD_REQUEST, "Ca làm việc không được để trống"),

    // Application & Office Errors
    RECRUITMENT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ ứng tuyển"),
    RECRUITMENT_APPLICATION_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "Email này đã nộp hồ sơ cho tin tuyển dụng này"),
    RECRUITMENT_OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bưu cục"),
    RECRUITMENT_MANAGER_OFFICE_NOT_FOUND(HttpStatus.BAD_REQUEST, "Không xác định được bưu cục của Branch Manager hiện tại"),

    // Status & Transition Errors
    RECRUITMENT_APPLICATION_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Chuyển trạng thái không hợp lệ"),
    RECRUITMENT_APPLICATION_NOT_APPROVED(HttpStatus.BAD_REQUEST, "Hồ sơ chưa ở trạng thái đã duyệt"),

    // Role & Permission Errors
    RECRUITMENT_PERMISSION_DENIED_MANAGE_JOB(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý tin tuyển dụng"),
    RECRUITMENT_PERMISSION_DENIED_REVIEW_APPLICATION(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt hồ sơ"),
    RECRUITMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiên thao tác này"),
    RECRUITMENT_PERMISSION_DENIED_VIEW_APPLICATION(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hồ sơ"),
    RECRUITMENT_PERMISSION_DENIED_OFFICE_ACCESS(HttpStatus.FORBIDDEN, "Chỉ được xem hồ sơ thuộc bưu cục của bạn"),
    RECRUITMENT_UNAUTHORIZED_ROLE(HttpStatus.UNAUTHORIZED, "Không xác định được role hiện tại"),

    // User/Account Errors
    SYSTEM_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy role hệ thống"),
    PHONE_NUMBER_USED_BY_OTHER(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}