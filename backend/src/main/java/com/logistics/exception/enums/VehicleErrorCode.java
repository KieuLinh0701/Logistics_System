package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VehicleErrorCode implements BaseErrorCode {
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phương tiện"),
    VEHICLE_LICENSE_PLATE_EXISTED(HttpStatus.BAD_REQUEST, "Biển số xe đã tồn tại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
