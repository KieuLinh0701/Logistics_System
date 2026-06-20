package com.logistics.exception;

import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {

    // Get validation result from exception
    // Retrieve all field validation errors
    // Convert the list of FieldError to a stream for processing
    // Extract the default error message from each FieldError
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.failure(
                CommonErrorCode.VALIDATION_ERROR.getMessage() + ": " + String.join(", ", errors)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<?>> handleBadRequest(HttpMessageNotReadableException ex) {

    Throwable cause = ex.getCause();

    if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.failure(CommonErrorCode.INVALID_ENUM_VALUE.getMessage()));
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.failure(CommonErrorCode.BAD_REQUEST.getMessage()));
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
    return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
        .body(ApiResponse.failure(ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }
}
