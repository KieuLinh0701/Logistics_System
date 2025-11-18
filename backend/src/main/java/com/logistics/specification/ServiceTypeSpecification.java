package com.logistics.specification;

import org.springframework.data.jpa.domain.Specification;
import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;

public class ServiceTypeSpecification {

    public static Specification<ServiceType> statusActive() {
        return (root, query, cb) ->
            cb.equal(root.get("status"), ServiceTypeStatus.ACTIVE);
    }

    

    public static Specification<ServiceType> filterByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}