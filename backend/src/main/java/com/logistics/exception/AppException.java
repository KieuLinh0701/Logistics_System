package com.logistics.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
  private final ErrorCode errorCode;

  public AppException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public AppException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AppException(ErrorCode errorCode, Object... args) {
    super(String.format(errorCode.getMessage(), args));
    this.errorCode = errorCode;
  }
}
