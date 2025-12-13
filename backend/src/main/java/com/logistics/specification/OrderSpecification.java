package com.logistics.specification;

import com.logistics.entity.Account;
import com.logistics.entity.Address;
import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {

    public static Specification<Order> unrestrictedOrder() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Order> userId(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Order> officeId(Integer officeId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("fromOffice").get("id"), officeId),
                cb.equal(root.get("toOffice").get("id"), officeId));
    }

    public static Specification<Order> payer(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("payer"), value);
        };
    }

    public static Specification<Order> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<Order> pickupType(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("pickupType"), value);
        };
    }

    public static Specification<Order> serviceTypeId(Integer value) {
        return (root, query, cb) -> {
            if (value == null) {
                return null;
            }
            return cb.equal(root.get("serviceType").get("id"), value);
        };
    }

    public static Specification<Order> paymentStatus(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("paymentStatus"), value);
        };
    }

    public static Specification<Order> cod(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }

            switch (value.toLowerCase()) {
                case "yes":
                    return cb.greaterThan(root.get("cod"), 0);
                case "no":
                    return cb.equal(root.get("cod"), 0);
                default:
                    return null;
            }
        };
    }

    public static Specification<Order> sort(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty() || query == null) {
                return null;
            }

            switch (value.toLowerCase()) {
                case "newest":
                    query.orderBy(cb.desc(root.get("createdAt")));
                    break;
                case "oldest":
                    query.orderBy(cb.asc(root.get("createdAt")));
                    break;
                case "cod_high":
                    query.orderBy(cb.desc(root.get("cod")));
                    break;
                case "cod_low":
                    query.orderBy(cb.asc(root.get("cod")));
                    break;
                case "order_value_high":
                    query.orderBy(cb.desc(root.get("orderValue")));
                    break;
                case "order_value_low":
                    query.orderBy(cb.asc(root.get("orderValue")));
                    break;
                case "fee_high":
                    query.orderBy(cb.desc(root.get("totalFee")));
                    break;
                case "fee_low":
                    query.orderBy(cb.asc(root.get("totalFee")));
                    break;
                case "weight_high":
                    query.orderBy(cb.desc(root.get("weight")));
                    break;
                case "weight_low":
                    query.orderBy(cb.asc(root.get("weight")));
                    break;
                default:
                    break;
            }

            return null;
        };
    }

    public static Specification<Order> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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

    public static Specification<Order> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;

            String likePattern = "%" + keyword.toLowerCase() + "%";
            Join<Order, Address> recipientAddr = root.join("recipientAddress", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("notes")), likePattern),
                    cb.like(cb.lower(root.get("trackingNumber")), likePattern),
                    cb.like(cb.lower(recipientAddr.get("name")), likePattern),
                    cb.like(cb.lower(recipientAddr.get("phoneNumber")), likePattern));
        };
    }

    public static Specification<Order> searchManager(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;

            String like = "%" + keyword.toLowerCase() + "%";

            Join<Order, Address> recipientAddr = root.join("recipientAddress", JoinType.LEFT);
            Join<Order, User> user = root.join("user", JoinType.LEFT);
            Join<User, Account> account = user.join("account", JoinType.LEFT);
            Join<Order, Employee> employee = root.join("employee", JoinType.LEFT);

            return cb.or (
                    cb.like(cb.lower(root.get("trackingNumber")), like),
                    cb.like(cb.lower(root.get("notes")), like),

                    cb.like(cb.lower(root.get("senderName")), like),
                    cb.like(cb.lower(root.get("senderPhone")), like),

                    cb.like(cb.lower(recipientAddr.get("name")), like),
                    cb.like(cb.lower(recipientAddr.get("phoneNumber")), like),

                    cb.like(cb.lower(user.get("code")), like),
                    cb.like(cb.lower(user.get("phoneNumber")), like),

                    cb.like(cb.lower(account.get("email")), like),

                    cb.like(cb.lower(employee.get("code")), like) 
            );
        };
    }

    public static Specification<Order> excludeDraft() {
    return (root, query, cb) ->
            cb.notEqual(root.get("status"), OrderStatus.DRAFT);
}

}