package com.logistics.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CloudinaryErrorCode implements BaseErrorCode {
    CLOUDINARY_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Upload ảnh thất bại"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
