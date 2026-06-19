package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VehicleErrorCode implements BaseErrorCode {
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Phương tiện không tồn tại"),
    VEHICLE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "Trạng thái phương tiện không hợp lệ"),
    VEHICLE_MAINTENANCE_DATE_INVALID(HttpStatus.BAD_REQUEST, "Ngày bảo trì tiếp theo phải lớn hơn hoặc bằng hiện tại"),
    VEHICLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không có quyền trên phương tiện này"),
    VEHICLE_OFFICE_MISMATCH(HttpStatus.FORBIDDEN, "Phương tiên không thuộc bưu cục của bạn!"),
    VEHICLE_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "Tổng trọng lượng vượt quá sức chứa của xe. Vui lòng chọn xe khác."),
    VEHICLE_LICENSE_PLATE_EXISTED(HttpStatus.BAD_REQUEST, "Biển số xe đã tồn tại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
