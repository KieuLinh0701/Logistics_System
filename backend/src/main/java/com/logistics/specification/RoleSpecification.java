package com.logistics.specification;

import com.logistics.entity.Role;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class RoleSpecification {

    public static Specification<Role> unrestrictedRole() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Role> currentShop(Integer userId) {
        return (root, query, cb) -> {

            return cb.equal(root.get("userOwner").get("id"), userId);
        };
    }

    public static Specification<Role> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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

    public static Specification<Role> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("description")), likePattern));
        };
    }
}