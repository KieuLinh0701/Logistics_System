import type {AttemptCategory, AttemptStatus} from "../types/attemptHistory";

export const ATTEMPT_CATEGORIES: AttemptCategory[] = [
    "PICKUP",
    "DELIVERY",
    "RETURN_DELIVERY",
];

export const ATTEMPT_STATUSES: AttemptStatus[] = ["SUCCESS", "FAILED"];

export const ATTEMPT_FILTER_SORT = ["NEWEST", "OLDEST"] as const;

export const translateAttemptCategory = (value: string | null | undefined): string => {
    switch (value) {
        case "PICKUP":
            return "Lấy hàng";
        case "DELIVERY":
            return "Giao hàng";
        case "RETURN_DELIVERY":
            return "Giao hoàn";
        default:
            return "N/A";
    }
};

export const translateAttemptStatus = (value: string | null | undefined): string => {
    switch (value) {
        case "SUCCESS":
            return "Thành công";
        case "FAILED":
            return "Thất bại";
        default:
            return "N/A";
    }
};

const PICKUP_FAIL_REASON_VI: Record<string, string> = {
    SHOP_CLOSED: "Cửa hàng đóng cửa",
    NOT_READY: "Hàng chưa sẵn sàng",
    CUSTOMER_CANCELLED: "Khách hàng huỷ",
    NO_RESPONSE: "Không liên lạc được",
    OTHER: "Khác",
};

const DELIVERY_FAIL_REASON_VI: Record<string, string> = {
    RECIPIENT_NOT_AVAILABLE: "Khách không có mặt",
    NO_RESPONSE: "Không liên lạc được",
    WRONG_ADDRESS: "Sai địa chỉ",
    RECIPIENT_REFUSED: "Khách từ chối nhận",
    RESCHEDULE_REQUESTED: "Khách hẹn giao lại",
    OTHER: "Khác",
    SENDER_NOT_AVAILABLE: "Không liên lạc được người gửi",
    SENDER_REFUSED: "Người gửi từ chối nhận lại",
    RETURN_ADDRESS_INVALID: "Sai địa chỉ hoàn trả",
    RETURN_RESCHEDULE_REQUESTED: "Người gửi hẹn giao lại sau",
};

export const translateFailReason = (
    reason: string | null | undefined,
    category?: string | null,
): string => {
    if (!reason) return "";
    if (category === "PICKUP") {
        return PICKUP_FAIL_REASON_VI[reason] || reason;
    }
    return DELIVERY_FAIL_REASON_VI[reason] || reason;
};

export const translateAttemptSort = (value: string | null | undefined): string => {
    switch (value) {
        case "OLDEST":
            return "Cũ nhất";
        case "NEWEST":
            return "Mới nhất";
        default:
            return value || "";
    }
};
