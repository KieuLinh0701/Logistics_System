package com.logistics.specification;

import com.logistics.entity.Vehicle;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class VehicleSpecification {

    public static Specification<Vehicle> unrestrictedVehicle() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Vehicle> officeId(Integer id) {
        return (root, query, cb) -> cb.equal(root.get("office").get("id"), id);
    }

    public static Specification<Vehicle> type(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("type"), value);
        };
    }

    public static Specification<Vehicle> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<Vehicle> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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

    public static Specification<Vehicle> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("licensePlate")), likePattern),
                    cb.like(cb.lower(root.get("description")), likePattern));
        };
    }
}