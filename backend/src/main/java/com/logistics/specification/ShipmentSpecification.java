package com.logistics.specification;

import com.logistics.entity.Employee;
import com.logistics.entity.Shipment;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class ShipmentSpecification {

    public static Specification<Shipment> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Shipment> fromOffice(Integer officeId) {
        return (root, query, cb) -> {
            if (officeId == null)
                return null;
            return cb.equal(root.get("fromOffice").get("id"), officeId);
        };
    }

    public static Specification<Shipment> employeeId(Integer id) {
        return (root, query, cb) -> {
            if (id == null)
                return null;
            return cb.equal(root.get("employee").get("id"), id);
        };
    }

    public static Specification<Shipment> status(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank())
                return null;

            ShipmentStatus parsed;
            try {
                parsed = ShipmentStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                return null;
            }

            return cb.equal(root.get("status"), parsed);
        };
    }

    public static Specification<Shipment> type(String type) {
        return (root, query, cb) -> {
            if (type == null || type.isBlank())
                return null;

            ShipmentType parsed;
            try {
                parsed = ShipmentType.valueOf(type.toUpperCase());
            } catch (Exception e) {
                return null;
            }

            return cb.equal(root.get("type"), parsed);
        };
    }

    public static Specification<Shipment> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;

            String like = "%" + keyword.toLowerCase() + "%";

            Join<Shipment, Employee> empJoin = root.join("employee", JoinType.LEFT);
            Join<?, ?> vehicleJoin = root.join("vehicle", JoinType.LEFT);

            Join<?, ?> userJoin = empJoin.join("user", JoinType.LEFT);
            Join<?, ?> accountJoin = userJoin.join("account", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), like), // mã shipment
                    cb.like(cb.lower(empJoin.get("code")), like), // mã nhân viên
                    cb.like(cb.lower(
                            cb.concat(userJoin.get("lastName"),
                                    cb.concat(" ", userJoin.get("firstName")))),
                            like),
                    cb.like(cb.lower(userJoin.get("phoneNumber")), like), // sđt
                    cb.like(cb.lower(accountJoin.get("email")), like), // email

                    cb.like(cb.lower(vehicleJoin.get("licensePlate")), like) // Biển số xe
            );
        };
    }

    public static Specification<Shipment> searchByCode(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String like = "%" + keyword.toLowerCase() + "%";

            return cb.like(
                    cb.lower(root.get("code")),
                    like);
        };
    }

    public static Specification<Shipment> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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