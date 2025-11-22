package com.logistics.specification;

import com.logistics.entity.Product;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> unrestrictedProduct() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Product> userId(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Product> type(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("type"), value);
        };
    }

    public static Specification<Product> status(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("status"), value);
        };
    }

    public static Specification<Product> stock(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty() || query == null) {
                return null;
            }

            switch (value.toLowerCase()) {
                case "instock":
                    query.orderBy(cb.desc(root.get("stock")));
                    return cb.greaterThan(root.get("stock"), 0);
                case "outofstock":
                    query.orderBy(cb.asc(root.get("stock")));
                    return cb.equal(root.get("stock"), 0);
                case "lowstock":
                    query.orderBy(cb.asc(root.get("stock")));
                    return cb.lessThan(root.get("stock"), 10);
                default:
                    break;
            }

            return null;
        };
    }

    public static Specification<Product> sort(String value) {
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
                case "bestselling":
                    query.orderBy(cb.desc(root.get("soldQuantity")));
                    break;
                case "leastselling":
                    query.orderBy(cb.asc(root.get("soldQuantity")));
                    break;
                case "highestprice":
                    query.orderBy(cb.desc(root.get("price")));
                    break;
                case "lowestprice":
                    query.orderBy(cb.asc(root.get("price")));
                    break;
                case "higheststock":
                    query.orderBy(cb.desc(root.get("stock")));
                    break;
                case "loweststock":
                    query.orderBy(cb.asc(root.get("stock")));
                    break;
                default:
                    break;
            }

            return null;
        };
    }

    public static Specification<Product> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
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

    public static Specification<Product> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("code")), likePattern));
        };
    }
}