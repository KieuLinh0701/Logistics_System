package com.logistics.specification;

import com.logistics.entity.Address;
import com.logistics.enums.AddressType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class RecipientAddressSpecification {

    public static Specification<Address> userId(int userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Address> type(AddressType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Address> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("phoneNumber")), like),
                    cb.like(cb.lower(root.get("fullAddress")), like)
            );
        };
    }

    public static Specification<Address> createdAtBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start == null) return cb.lessThanOrEqualTo(root.get("createdAt"), end);
            if (end == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.between(root.get("createdAt"), start, end);
        };
    }
}
