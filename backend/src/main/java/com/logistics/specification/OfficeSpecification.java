package com.logistics.specification;

import org.springframework.data.jpa.domain.Specification;

import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;

public class OfficeSpecification {
    public static Specification<Office> active() {
        return (root, query, cb) ->
            cb.equal(root.get("status"), OfficeStatus.ACTIVE);
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
