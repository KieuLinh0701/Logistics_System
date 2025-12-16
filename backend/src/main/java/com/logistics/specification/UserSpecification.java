package com.logistics.specification;

import com.logistics.entity.AccountRole;
import com.logistics.entity.Employee;
import com.logistics.entity.User;
import com.logistics.enums.EmployeeStatus;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class UserSpecification {

    public static Specification<User> fetchAccount() {
        return (root, query, cb) -> {
            root.fetch("account", JoinType.LEFT);
            return cb.conjunction();
        };
    }

    public static Specification<User> fetchEmployees() {
        return (root, query, cb) -> {
            root.fetch("employees", JoinType.LEFT);
            return cb.conjunction();
        };
    }

    public static Specification<User> accountIsActive() {
        return (root, query, cb) -> cb.isTrue(root.get("account").get("isActive"));
    }

    public static Specification<User> roleShipperActive() {
        return (root, query, cb) -> {
            Join<User, AccountRole> roleJoin = root.join("account").join("accountRoles", JoinType.LEFT);
            return cb.and(
                    cb.equal(roleJoin.get("role").get("name"), "Shipper"),
                    cb.isTrue(roleJoin.get("isActive")));
        };
    }

    // 5. Predicate: employee thuộc office
    public static Specification<User> employeeInOffice(Integer officeId) {
        return (root, query, cb) -> {
            Join<User, Employee> empJoin = root.join("employees", JoinType.LEFT);
            return cb.equal(empJoin.get("office").get("id"), officeId);
        };
    }

    // 6. Predicate: employee đang làm việc
    public static Specification<User> employeeIsWorking() {
        return (root, query, cb) -> {
            Join<User, Employee> empJoin = root.join("employees", JoinType.LEFT);
            return cb.notEqual(empJoin.get("status"), EmployeeStatus.LEAVE);
        };
    }

    // 7. Predicate: search
    public static Specification<User> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String likePattern = "%" + keyword.trim().toLowerCase() + "%";

            Join<User, Employee> empJoin = root.join("employees", JoinType.LEFT);

            Expression<String> fullName = cb.concat(cb.lower(root.get("lastName")), " ");
            fullName = cb.concat(fullName, cb.lower(root.get("firstName")));

            return cb.or(
                    cb.like(cb.lower(empJoin.get("code")), likePattern), 
                    cb.like(fullName, likePattern), 
                    cb.like(cb.lower(root.get("account").get("email")), likePattern),
                    cb.like(cb.lower(root.get("phoneNumber")), likePattern));
        };
    }

    // 8. Lấy tất cả Shipper trong office (status không quan tâm)
    public static Specification<User> allShippersInOffice(Integer officeId, String search) {
        return Specification.where(fetchAccount())
                .and(fetchEmployees())
                .and(accountIsActive())
                .and(roleShipperActive())
                .and(employeeInOffice(officeId))
                .and(search(search));
    }

    // 9. Lấy Shipper đang làm việc (status != LEAVE)
    public static Specification<User> activeShippersInOffice(Integer officeId, String search) {
        return allShippersInOffice(officeId, search)
                .and(employeeIsWorking());
    }

    // 10. ShipperAssignment còn hiệu lực
    public static Specification<com.logistics.entity.ShipperAssignment> activeAssignment() {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.or(
                    cb.isNull(root.get("endAt")),
                    cb.greaterThanOrEqualTo(root.get("endAt"), now));
        };
    }
}