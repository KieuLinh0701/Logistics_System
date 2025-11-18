package com.logistics.specification;

import org.springframework.data.jpa.domain.Specification;

import com.logistics.entity.Office;

public class OfficeSpecification {
    public static Specification<Office> status(String status) {
        return (root, query, cb) ->
            cb.equal(root.get("status"), status);
    }

    public static Specification<Office> type(String type) {
        return (root, query, cb) ->
            cb.equal(root.get("type"), type);
    }

    public static Specification<Office> city(Integer cityCode) {
        return (root, query, cb) ->
            cityCode == null ? null :
            cb.equal(root.get("cityCode"), cityCode);
    }

    public static Specification<Office> ward(Integer wardCode) {
        return (root, query, cb) ->
            wardCode == null ? null :
            cb.equal(root.get("wardCode"), wardCode);
    }

    public static Specification<Office> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("detail")), pattern)
            );
        };
    }
}
