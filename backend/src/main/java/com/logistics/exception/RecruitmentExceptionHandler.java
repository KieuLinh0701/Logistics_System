package com.logistics.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.logistics.controller.recruitment.RecruitmentController;
import com.logistics.response.ApiResponse;

@ControllerAdvice(assignableTypes = RecruitmentController.class)
public class RecruitmentExceptionHandler {

    @ExceptionHandler(RecruitmentException.class)
    public ResponseEntity<ApiResponse<Object>> handleRecruitmentException(RecruitmentException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(
                        new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Lỗi hệ thống: " + ex.getMessage(), null));
    }
}
