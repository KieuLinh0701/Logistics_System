package com.logistics.exception;

public class LeaveException extends RuntimeException {
    public LeaveException(String message) {
        super(message);
    }
}