package com.logistics.service.chat;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class SupportIntentDetector {

    public enum Intent {
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

    public Intent detect(String message) {
        if (message == null || message.isBlank()) {
            return Intent.NONE;
        }

        String text = normalize(message);

        if (containsAny(text,
                "liên hệ manager",
                "gặp manager",
                "gặp quản lý",
                "liên hệ quản lý",
                "gặp nhân viên",
                "gặp người thật",
                "cần nhân viên hỗ trợ",
                "cần hỗ trợ viên",
                "gặp cskh",
                "liên hệ cskh",
                "gặp tư vấn viên",
                "nói chuyện với người thật",
                "chuyển tôi cho quản lý",
                "khiếu nại",
                "hoàn tiền",
                "đổi địa chỉ",
                "mất hàng",
                "sai cod",
                "bồi thường",
                "giao chậm",
                "hủy đơn",
                "không nhận được hàng")) {
            return Intent.FALLBACK_TO_HUMAN;
        }

        if (containsAny(text, "chào", "xin chào", "hello", "hi", "alo", "bạn ơi", "ad ơi")) {
            return Intent.GREETING;
        }

        if (containsAny(text, "lịch sử", "hành trình", "đã qua đâu", "timeline", "quá trình")) {
            return Intent.ORDER_HISTORY;
        }

        if (containsAny(text, "ai đang giao", "ai giao", "shipper", "người giao", "tài xế", "nhân viên giao hàng")) {
            return Intent.SHIPPER_INFO;
        }

        if (containsAny(text, "cod", "tiền thu hộ", "thu hộ", "thanh toán")) {
            return Intent.COD_INFO;
        }

        if (containsAny(text, "thông tin", "chi tiết", "xem đơn", "người nhận", "địa chỉ", "khối lượng", "phí")) {
            return Intent.ORDER_DETAIL;
        }

        if (containsAny(text, "trạng thái", "tới đâu", "đang ở đâu", "khi nào giao", "eta", "giao chưa", "đơn này sao rồi", "đã giao chưa")) {
            return Intent.ORDER_STATUS;
        }

        if (looksLikeTrackingNumber(text)) {
            return Intent.ORDER_STATUS;
        }

        if (containsAny(text, "cảm ơn", "thanks", "thank you", "ok cảm ơn", "dạ cảm ơn")) {
            return Intent.THANK_YOU;
        }

        return Intent.NONE;
    }

    public boolean containsTrackingCandidate(String message) {
        return looksLikeTrackingNumber(normalize(message));
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeTrackingNumber(String text) {
        return text.matches(".*\\b[a-z]{2,6}[-_ ]?\\d{3,}\\b.*");
    }
}
