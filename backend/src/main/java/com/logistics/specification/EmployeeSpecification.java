package com.logistics.specification;

import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.Employee;
import com.logistics.entity.Role;
import com.logistics.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecification {

    public static Specification<Employee> unrestrictedEmployee() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Employee> officeId(Integer officeId) {
        return (root, query, cb) -> cb.equal(root.get("office").get("id"), officeId);
    }

    public static Specification<Employee> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<Employee> excludeStatus(String statusToExclude) {
        return (root, query, cb) -> {
            if (statusToExclude == null || statusToExclude.isEmpty()) {
                return null; 
            }
            return cb.notEqual(root.get("status"), statusToExclude);
        };
    }

    public static Specification<Employee> shift(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("shift"), value);
        };
    }

    public static Specification<Employee> role(String value, boolean excludeManager) {
        return (root, query, cb) -> {
            query.distinct(true);

            Join<Employee, AccountRole> accountRoleJoin = root.join("accountRole", JoinType.LEFT);
            Join<AccountRole, Role> roleJoin = accountRoleJoin.join("role", JoinType.LEFT);

            Predicate predicate = cb.conjunction();

            if (value != null && !value.isEmpty()) {
                predicate = cb.and(
                        predicate,
                        cb.equal(cb.lower(roleJoin.get("name")), value.toLowerCase()));
            }

            if (excludeManager) {
                predicate = cb.and(
                        predicate,
                        cb.notEqual(cb.lower(roleJoin.get("name")), "manager"));
            }

            return predicate;
        };
    }

    public static Specification<Employee> hireDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("hireDate"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("hireDate"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("hireDate"), endDate);
            }
        };
    }

    public static Specification<Employee> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String likePattern = "%" + keyword.toLowerCase() + "%";

            Join<Employee, User> userJoin = root.join("user", JoinType.LEFT);
            Join<User, Account> accountJoin = userJoin.join("account", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), likePattern), // mã nhân viên
                    cb.like(cb.lower(cb.concat(userJoin.get("lastName"), cb.concat(" ", userJoin.get("firstName")))),
                            likePattern),
                    cb.like(cb.lower(accountJoin.get("email")), likePattern), // email
                    cb.like(cb.lower(userJoin.get("phoneNumber")), likePattern) // số điện thoại
            );
        };
    }
}