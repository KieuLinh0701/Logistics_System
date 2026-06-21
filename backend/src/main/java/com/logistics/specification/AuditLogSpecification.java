package com.logistics.specification;

import com.logistics.entity.*;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.EntityType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<AuditLog> office(Integer officeId) {
        return (root, query, cb) -> officeId == null ? null : cb.equal(root.get("office").get("id"), officeId);
    }

    public static Specification<AuditLog> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("entityId")), pattern)
            );
        };
    }

    public static Specification<AuditLog> status(AuditLogStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<AuditLog> entityType(EntityType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("entity"), type);
    }

    public static Specification<AuditLog> action(AuditLogAction action) {
        return (root, query, cb) -> action == null ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> createdAtBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null) return cb.between(root.get("createdAt"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }

    public static Specification<AuditLog> role(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.isBlank()) return null;

            Join<AuditLog, User> userJoin = root.join("user", JoinType.INNER);

            Join<User, Employee> employeeJoin = userJoin.join("employees", JoinType.INNER);

            Join<Employee, AccountRole> accountRoleJoin = employeeJoin.join("accountRole", JoinType.INNER);
            Join<AccountRole, Role> roleJoin = accountRoleJoin.join("role", JoinType.INNER);

            Predicate rolePredicate = cb.equal(roleJoin.get("name"), roleName);
            Predicate statusPredicate = cb.equal(employeeJoin.get("status"), EmployeeStatus.ACTIVE);

            assert query != null;
            query.distinct(true);

            return cb.and(rolePredicate, statusPredicate);
        };
    }
}