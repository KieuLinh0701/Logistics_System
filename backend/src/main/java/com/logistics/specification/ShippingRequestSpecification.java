package com.logistics.specification;

import com.logistics.entity.ShippingRequest;

import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class ShippingRequestSpecification {

    public static Specification<ShippingRequest> unrestrictedShippingRequest() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<ShippingRequest> userId(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<ShippingRequest> officeId(Integer officeId) {
        return (root, query, cb) -> cb.equal(root.get("office").get("id"), officeId);
    }

    public static Specification<ShippingRequest> requestType(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("requestType"), value);
        };
    }

    public static Specification<ShippingRequest> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<ShippingRequest> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";

            var orderJoin = root.join("order", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern),
                    cb.like(cb.lower(root.get("response")), likePattern),
                    cb.like(cb.lower(root.get("code")), likePattern),
                    cb.like(cb.lower(root.get("requestContent")), likePattern));
        };
    }

    public static Specification<ShippingRequest> managerSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";

            var orderJoin = root.join("order", JoinType.LEFT);
            var userJoin = root.join("user", JoinType.LEFT);
            var accountJoin = userJoin.join("account", JoinType.LEFT);

            var fullNameExpression = cb.concat(cb.concat(cb.lower(userJoin.get("lastName")), " "),
                    cb.lower(userJoin.get("firstName")));

            return cb.or(
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern),
                    cb.like(cb.lower(userJoin.get("code")), likePattern),
                    cb.like(fullNameExpression, likePattern), 
                    cb.like(cb.lower(accountJoin.get("email")), likePattern), 
                    cb.like(cb.lower(userJoin.get("phoneNumber")), likePattern),
                    cb.like(cb.lower(root.get("contactName")), likePattern),
                    cb.like(cb.lower(root.get("contactEmail")), likePattern),
                    cb.like(cb.lower(root.get("contactPhoneNumber")), likePattern),
                    cb.like(cb.lower(root.get("response")), likePattern),
                    cb.like(cb.lower(root.get("code")), likePattern),
                    cb.like(cb.lower(root.get("requestContent")), likePattern));
        };
    }

    public static Specification<ShippingRequest> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("paidAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("paidAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("paidAt"), endDate);
            }
        };
    }

    public static Specification<ShippingRequest> responseAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("responseAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("responseAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("responseAt"), endDate);
            }
        };
    }
}