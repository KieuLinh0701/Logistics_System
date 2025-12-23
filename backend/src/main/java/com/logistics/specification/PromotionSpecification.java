package com.logistics.specification;

import org.springframework.data.jpa.domain.Specification;
import com.logistics.entity.Promotion;
import com.logistics.enums.PromotionStatus;

import java.time.LocalDateTime;

public class PromotionSpecification {

    public static Specification<Promotion> status(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Promotion> isActiveNow() {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.and(
                    cb.lessThanOrEqualTo(root.get("startDate"), now),
                    cb.greaterThanOrEqualTo(root.get("endDate"), now));
        };
    }

    public static Specification<Promotion> hasRemainingUsage() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("usageLimit")),
                cb.greaterThan(root.get("usageLimit"), root.get("usedCount")));
    }

    public static Specification<Promotion> activeAndUsable() {
        return Specification.<Promotion>unrestricted()
                .and(status(PromotionStatus.ACTIVE.name()))
                .and(isActiveNow())
                .and(hasRemainingUsage());
    }
}