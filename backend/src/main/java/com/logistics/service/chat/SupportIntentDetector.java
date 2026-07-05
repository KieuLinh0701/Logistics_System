package com.logistics.service.chat;

public interface SupportIntentDetector {
    Intent detect(String message);
    boolean containsTrackingCandidate(String message);

    enum Intent {
        ORDER_STATUS,
        ORDER_DETAIL,
        ORDER_HISTORY,
        COD_INFO,
        SHIPPER_INFO,
        FALLBACK_TO_HUMAN,
        GREETING,
        THANK_YOU,
        NONE
    }
}
