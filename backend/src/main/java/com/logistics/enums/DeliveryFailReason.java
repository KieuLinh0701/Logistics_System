package com.logistics.enums;

public enum DeliveryFailReason {
    RECIPIENT_NOT_AVAILABLE,
    NO_RESPONSE,
    WRONG_ADDRESS,
    RECIPIENT_REFUSED,
    RESCHEDULE_REQUESTED,
    OTHER,
    // Return delivery fail reasons
    SENDER_NOT_AVAILABLE,
    SENDER_REFUSED,
    RETURN_ADDRESS_INVALID,
    RETURN_RESCHEDULE_REQUESTED
}
