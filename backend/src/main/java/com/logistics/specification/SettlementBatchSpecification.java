package com.logistics.specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class SettlementBatchSpecification {

    public static Specification<SettlementBatch> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<SettlementBatch> userId(Integer id) {
        return (root, query, cb) -> {
            if (id == null)
                return null;
            return cb.equal(root.get("shop").get("id"), id);
        };
    }

    public static Specification<SettlementBatch> status(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<SettlementBatch> balanceType(String type) {
        return (root, query, cb) -> {
            if (type == null)
                return null;

            switch (type.toLowerCase()) {
                case "balanced":
                    return cb.equal(root.get("balanceAmount"), BigDecimal.ZERO);

                case "system_pays":
                    return cb.greaterThan(root.get("balanceAmount"), BigDecimal.ZERO);

                case "shop_pays":
                    return cb.lessThan(root.get("balanceAmount"), BigDecimal.ZERO);

                default:
                    return null;
            }
        };
    }

    public static Specification<SettlementBatch> createdAtBetween(LocalDateTime startDate,
            LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
        };
    }

    public static Specification<SettlementBatch> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            query.distinct(true);

            String likePattern = "%" + keyword.toLowerCase() + "%";

            // Đơn hàng
            Join<SettlementBatch, Order> orderJoin = root.join("orders", JoinType.LEFT);

            return cb.or(
                    // Mã đối soát batch
                    cb.like(cb.lower(root.get("code")), likePattern),

                    // Mã đơn hàng
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern));
        };
    }

}