package com.logistics.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
  private final BaseErrorCode errorCode;

  public AppException(BaseErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public AppException(BaseErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AppException(BaseErrorCode errorCode, Object... args) {
    super(String.format(errorCode.getMessage(), args));
    this.errorCode = errorCode;
  }
}
