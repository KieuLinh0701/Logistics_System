package com.logistics.specification;

import com.logistics.entity.IncidentReport;

import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class IncidentReportSpecification {

    public static Specification<IncidentReport> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<IncidentReport> officeId(Integer officeId) {
        return (root, query, cb) -> cb.equal(root.get("office").get("id"), officeId);
    }

    public static Specification<IncidentReport> incidentType(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("incidentType"), value);
        };
    }

    public static Specification<IncidentReport> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<IncidentReport> priority(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("priority"), value);
        };
    }

    public static Specification<IncidentReport> managerSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";

            // Join với đơn hàng
            var orderJoin = root.join("order", JoinType.LEFT);

            // Join shipper và handler
            var shipperJoin = root.join("shipper", JoinType.LEFT);
            var handlerJoin = root.join("handler", JoinType.LEFT);

            // fullName shipper
            var shipperFullName = cb.concat(cb.concat(cb.lower(shipperJoin.get("lastName")), " "),
                    cb.lower(shipperJoin.get("firstName")));

            // fullName handler
            var handlerFullName = cb.concat(cb.concat(cb.lower(handlerJoin.get("lastName")), " "),
                    cb.lower(handlerJoin.get("firstName")));

            return cb.or(
                    cb.like(cb.lower(root.get("code")), likePattern), // Mã sự cố
                    cb.like(cb.lower(root.get("title")), likePattern), // Tiêu đề
                    cb.like(cb.lower(root.get("description")), likePattern), // Miêu tả
                    cb.like(shipperFullName, likePattern), // Tên shipper
                    cb.like(cb.lower(shipperJoin.get("phoneNumber")), likePattern), // SĐT shipper
                    cb.like(handlerFullName, likePattern), // Tên handler
                    cb.like(cb.lower(handlerJoin.get("phoneNumber")), likePattern), // SĐT handler
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern) // Mã đơn hàng
            );
        };
    }

    public static Specification<IncidentReport> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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
}