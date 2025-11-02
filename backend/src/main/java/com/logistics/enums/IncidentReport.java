package com.logistics.enums;

public class IncidentReport {
    public enum IncidentType {
        RECIPIENT_NOT_AVAILABLE,
        WRONG_ADDRESS,
        PACKAGE_DAMAGED,
        RECIPIENT_REFUSED,
        SECURITY_ISSUE,
        OTHER
    }

    public enum IncidentPriority {
        LOW, MEDIUM, HIGH
    }

    public enum IncidentStatus {
        PENDING, PROCESSING, RESOLVED, REJECTED
    }
}
