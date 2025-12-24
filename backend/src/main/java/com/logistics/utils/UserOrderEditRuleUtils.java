package com.logistics.utils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.logistics.enums.OrderStatus;

public class UserOrderEditRuleUtils {

    // Các trường được thay đổi của user theo các trạng thái
    public static class EditableRule {
        private Set<OrderStatus> editableStatuses;
        private Set<OrderStatus> nonEditableStatuses;

        public EditableRule(Set<OrderStatus> editableStatuses, Set<OrderStatus> nonEditableStatuses) {
            this.editableStatuses = editableStatuses != null ? editableStatuses : Collections.emptySet();
            this.nonEditableStatuses = nonEditableStatuses != null ? nonEditableStatuses : Collections.emptySet();
        }

        public Set<OrderStatus> getEditableStatuses() {
            return editableStatuses;
        }

        public Set<OrderStatus> getNonEditableStatuses() {
            return nonEditableStatuses;
        }
    }

    public static final Set<OrderStatus> USER_FINAL_STATUSES = EnumSet.of(
            OrderStatus.DELIVERING, OrderStatus.DELIVERED, OrderStatus.RETURNED,
            OrderStatus.CANCELLED, OrderStatus.FAILED_DELIVERY);

    public static boolean canEditUserOrder(OrderStatus status) {
        return !USER_FINAL_STATUSES.contains(status);
    }

    // Các trạng thái cho phép sửa recipient
    public static final Set<OrderStatus> EDITABLE_RECIPIENT_STATUSES = EnumSet.of(
            OrderStatus.DRAFT, OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.READY_FOR_PICKUP, OrderStatus.PICKING_UP,
            OrderStatus.PICKED_UP, OrderStatus.AT_ORIGIN_OFFICE);

    public static final Map<String, EditableRule> USER_ORDER_FIELD_EDIT_RULES;
    static {
        Map<String, EditableRule> map = new HashMap<>();

        map.put("senderAddress", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("senderName", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("senderPhoneNumber", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("senderCityCode", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("senderWardCode", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("senderDetailAddress", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));

        // Thông tin recipient dùng chung
        map.put("recipientName", new EditableRule(EDITABLE_RECIPIENT_STATUSES, USER_FINAL_STATUSES));
        map.put("recipientPhoneNumber", new EditableRule(EDITABLE_RECIPIENT_STATUSES, USER_FINAL_STATUSES));
        map.put("recipientCityCode", new EditableRule(EDITABLE_RECIPIENT_STATUSES, USER_FINAL_STATUSES));
        map.put("recipientWardCode", new EditableRule(EDITABLE_RECIPIENT_STATUSES, USER_FINAL_STATUSES));
        map.put("recipientDetailAddress", new EditableRule(EDITABLE_RECIPIENT_STATUSES, USER_FINAL_STATUSES));

        // Các field khác
        map.put("weight", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("serviceType", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("cod", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("orderValue", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("products", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("pickupType", new EditableRule(EnumSet.of(OrderStatus.DRAFT, OrderStatus.PENDING), USER_FINAL_STATUSES));
        map.put("fromOffice",
                new EditableRule(EnumSet.of(OrderStatus.DRAFT, OrderStatus.PENDING), USER_FINAL_STATUSES));
        map.put("payer", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));
        map.put("notes", new EditableRule(EDITABLE_RECIPIENT_STATUSES,
                EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED)));
        map.put("promotion", new EditableRule(EnumSet.of(OrderStatus.DRAFT), USER_FINAL_STATUSES));

        USER_ORDER_FIELD_EDIT_RULES = Collections.unmodifiableMap(map);
    }

    public static boolean canEditUserOrderField(String field, OrderStatus status) {
        EditableRule rule = USER_ORDER_FIELD_EDIT_RULES.get(field);
        if (rule == null)
            return false;
        if (!rule.getNonEditableStatuses().isEmpty()) {
            return !rule.getNonEditableStatuses().contains(status);
        }
        if (!rule.getEditableStatuses().isEmpty()) {
            return rule.getEditableStatuses().contains(status);
        }
        return false;
    }

    private static final Set<String> USER_EDITABLE_ORDER_STATUSES = Set.of("DRAFT");

    public static boolean canEditUserOrderStatus(String status) {
        return USER_EDITABLE_ORDER_STATUSES.contains(status);
    }

}