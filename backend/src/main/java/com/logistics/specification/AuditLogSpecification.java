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

    public static Specification<AuditLog> user(Integer userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<AuditLog> office(Integer officeId) {
        return (root, query, cb) -> officeId == null ? null : cb.equal(root.get("office").get("id"), officeId);
    }

    public static Specification<AuditLog> shop(Integer shopId) {
        return (root, query, cb) -> shopId == null ? null : cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<AuditLog> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;

            String pattern = "%" + keyword + "%";

            Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%");
            Predicate entityIdPredicate = cb.like(root.get("entityId").as(String.class), pattern);

            return cb.or(descriptionPredicate, entityIdPredicate);
        };
    }

    public static Specification<AuditLog> searchManagerUser(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";

            Predicate descriptionPred = cb.like(cb.function("LOWER", String.class, root.get("description")), pattern);
            Predicate entityIdPred = cb.like(cb.function("LOWER", String.class, root.get("entityId")), pattern);

            var userSubquery = query.subquery(Integer.class);
            var userRoot = userSubquery.from(User.class);
            userSubquery.select(userRoot.get("id"))
                    .where(cb.and(
                            cb.equal(userRoot.get("id"), root.get("user").get("id")),
                            cb.or(
                                    cb.like(cb.function("LOWER", String.class, userRoot.get("firstName")), pattern),
                                    cb.like(cb.function("LOWER", String.class, userRoot.get("lastName")), pattern),
                                    cb.like(
                                            cb.function("LOWER", String.class,
                                                    cb.concat(cb.concat(userRoot.get("lastName"), " "), userRoot.get("firstName"))),
                                            pattern
                                    ),
                                    cb.like(userRoot.get("phoneNumber"), pattern)
                            )
                    ));

            return cb.or(
                    descriptionPred,
                    entityIdPred,
                    cb.exists(userSubquery)
            );
        };
    }

    public static Specification<AuditLog> searchAdmin(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";

            Predicate descriptionPred = cb.like(cb.function("LOWER", String.class, root.get("description")), pattern);
            Predicate entityIdPred = cb.like(cb.function("LOWER", String.class, root.get("entityId")), pattern);

            var userSubquery = query.subquery(Integer.class);
            var userRoot = userSubquery.from(User.class);
            userSubquery.select(userRoot.get("id"))
                    .where(cb.and(
                            cb.equal(userRoot.get("id"), root.get("user").get("id")),
                            cb.or(
                                    cb.like(cb.function("LOWER", String.class, userRoot.get("firstName")), pattern),
                                    cb.like(cb.function("LOWER", String.class, userRoot.get("lastName")), pattern),
                                    cb.like(
                                            cb.function("LOWER", String.class,
                                                    cb.concat(cb.concat(userRoot.get("lastName"), " "), userRoot.get("firstName"))),
                                            pattern
                                    ),
                                    cb.like(userRoot.get("phoneNumber"), pattern)
                            )
                    ));

            var officeSubquery = query.subquery(Integer.class);
            var officeRoot = officeSubquery.from(Office.class);
            officeSubquery.select(officeRoot.get("id"))
                    .where(cb.and(
                            cb.isNotNull(root.get("office")),
                            cb.equal(officeRoot.get("id"), root.get("office").get("id")),
                            cb.or(
                                    cb.like(cb.function("LOWER", String.class, officeRoot.get("name")), pattern),
                                    cb.like(cb.function("LOWER", String.class, officeRoot.get("code")), pattern),
                                    cb.like(officeRoot.get("phoneNumber"), pattern)
                            )
                    ));

            return cb.or(
                    descriptionPred,
                    entityIdPred,
                    cb.exists(userSubquery),
                    cb.exists(officeSubquery)
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

            Predicate ownerNullPredicate = cb.isNull(roleJoin.get("userOwner"));

            Predicate statusPredicate = cb.equal(employeeJoin.get("status"), EmployeeStatus.ACTIVE);

            assert query != null;
            query.distinct(true);

            return cb.and(rolePredicate, ownerNullPredicate, statusPredicate);
        };
    }

    public static Specification<AuditLog> role(String roleName, Integer shopId) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.isBlank()) return null;

            Join<AuditLog, User> userJoin = root.join("user", JoinType.INNER);
            Join<User, Employee> employeeJoin = userJoin.join("employees", JoinType.INNER);
            Join<Employee, AccountRole> accountRoleJoin = employeeJoin.join("accountRole", JoinType.INNER);
            Join<AccountRole, Role> roleJoin = accountRoleJoin.join("role", JoinType.INNER);

            Predicate rolePredicate = cb.equal(roleJoin.get("name"), roleName);
            Predicate statusPredicate = cb.equal(employeeJoin.get("status"), EmployeeStatus.ACTIVE);

            Predicate ownerPredicate = cb.or(cb.isNull(roleJoin.get("userOwner")), cb.equal(roleJoin.get("userOwner").get("id"), shopId));

            assert query != null;
            query.distinct(true);

            return cb.and(rolePredicate, ownerPredicate, statusPredicate);
        };
    }
}