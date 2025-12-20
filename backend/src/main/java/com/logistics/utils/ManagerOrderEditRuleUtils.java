package com.logistics.utils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderStatus;

public class ManagerOrderEditRuleUtils {
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

    public static final Set<OrderStatus> MANAGER_FINAL_STATUSES = EnumSet.of(
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED,
            OrderStatus.CANCELLED,
            OrderStatus.FAILED_DELIVERY
    );

    public static boolean canEditManagerOrder(OrderStatus status) {
        return !MANAGER_FINAL_STATUSES.contains(status);
    }

    
    // Đối với các đơn do user tạo
    public static final Map<String, EditableRule> MANAGER_EDIT_USER_ORDER_FIELD_RULES;
    static {
        Map<String, EditableRule> map = new HashMap<>();

        // Người gửi
        map.put("senderName", new EditableRule(null, null));
        map.put("senderPhoneNumber", new EditableRule(null, null));
        map.put("senderCityCode", new EditableRule(null, null));
        map.put("senderWardCode", new EditableRule(null, null));
        map.put("senderDetailAddress", new EditableRule(null, null));

        // Người nhận
        map.put("recipientName", new EditableRule(null, MANAGER_FINAL_STATUSES));
        map.put("recipientPhoneNumber", new EditableRule(null, MANAGER_FINAL_STATUSES));
        map.put("recipientCityCode", new EditableRule(null, null));
        map.put("recipientWardCode",
                new EditableRule(
                        EnumSet.of(
                                OrderStatus.PENDING,
                                OrderStatus.CONFIRMED,
                                OrderStatus.READY_FOR_PICKUP
                        ),
                        null
                )
        );
        map.put("recipientDetailAddress", new EditableRule(null, MANAGER_FINAL_STATUSES));

        // Thông tin đơn hàng 
        map.put("weight", new EditableRule(null, null));
        map.put("serviceType", new EditableRule(null, null));
        map.put("cod", new EditableRule(null, null));
        map.put("orderValue", new EditableRule(null, null));
        map.put("products", new EditableRule(null, null));

        // Lấy hàng
        map.put("pickupType",
                new EditableRule(EnumSet.of(OrderStatus.PENDING), null));
        map.put("fromOffice",
                new EditableRule(EnumSet.of(OrderStatus.PENDING), null));

        // Người trả phí
        map.put("payer", new EditableRule(null, null));

        // Ghi chú
        map.put("notes",
                new EditableRule(null, MANAGER_FINAL_STATUSES));

        // Khuyến mãi
        map.put("promotion", new EditableRule(null, null));

        MANAGER_EDIT_USER_ORDER_FIELD_RULES = Collections.unmodifiableMap(map);
    }

    // Đối với các đơn do đến bưu cục tạo (Admin / Manager)
    public static final Map<String, EditableRule> MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES;
    static {
        Map<String, EditableRule> map = new HashMap<>();

        // Người gửi
        map.put("senderName",
                new EditableRule(EnumSet.of(OrderStatus.AT_ORIGIN_OFFICE), null));
        map.put("senderPhoneNumber",
                new EditableRule(EnumSet.of(OrderStatus.AT_ORIGIN_OFFICE), null));
        map.put("senderCityCode", new EditableRule(null, null));
        map.put("senderWardCode",
                new EditableRule(EnumSet.of(OrderStatus.AT_ORIGIN_OFFICE), null));
        map.put("senderDetailAddress",
                new EditableRule(EnumSet.of(OrderStatus.AT_ORIGIN_OFFICE), null));

        // Người nhận
        map.put("recipientName", new EditableRule(null, MANAGER_FINAL_STATUSES));
        map.put("recipientPhoneNumber", new EditableRule(null, MANAGER_FINAL_STATUSES));
        map.put("recipientCityCode", new EditableRule(null, null));
        map.put("recipientWardCode",
                new EditableRule(
                        EnumSet.of(
                                OrderStatus.PENDING,
                                OrderStatus.CONFIRMED,
                                OrderStatus.READY_FOR_PICKUP,
                                OrderStatus.PICKING_UP,
                                OrderStatus.PICKED_UP,
                                OrderStatus.AT_ORIGIN_OFFICE
                        ),
                        null
                )
        );
        map.put("recipientDetailAddress", new EditableRule(null, MANAGER_FINAL_STATUSES));

        // Thông tin đơn hàng
        map.put("weight", new EditableRule(null, null));
        map.put("serviceType", new EditableRule(null, null));
        map.put("cod", new EditableRule(null, null));
        map.put("orderValue", new EditableRule(null, null));
        map.put("products", new EditableRule(null, null));

        // Hình thức lấy hàng
        map.put("pickupType", new EditableRule(null, null));
        map.put("fromOffice", new EditableRule(null, null));

        // Người trả phí
        map.put("payer", new EditableRule(null, null));

        // Ghi chú
        map.put("notes",
                new EditableRule(
                        null,
                        EnumSet.of(
                                OrderStatus.DELIVERED,
                                OrderStatus.CANCELLED,
                                OrderStatus.RETURNED
                        )
                )
        );

        // Khuyến mãi
        map.put("promotion", new EditableRule(null, null));

        MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES = Collections.unmodifiableMap(map);
    }

    public static boolean canManagerEditOrderField(
            String field,
            OrderStatus status,
            OrderCreatorType creatorType
    ) {
        Map<String, EditableRule> rules =
                creatorType == OrderCreatorType.USER
                        ? MANAGER_EDIT_USER_ORDER_FIELD_RULES
                        : MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES;

        EditableRule rule = rules.get(field);
        if (rule == null) return false;

        if (!rule.getNonEditableStatuses().isEmpty()) {
            return !rule.getNonEditableStatuses().contains(status);
        }

        if (!rule.getEditableStatuses().isEmpty()) {
            return rule.getEditableStatuses().contains(status);
        }

        return false;
    }
}