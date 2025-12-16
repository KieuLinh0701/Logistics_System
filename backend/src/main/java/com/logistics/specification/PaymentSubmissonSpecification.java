package com.logistics.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class PaymentSubmissonSpecification {

    public static Specification<PaymentSubmission> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<PaymentSubmission> batchId(Integer batchId) {
        return (root, query, cb) -> {
            if (batchId == null)
                return null;
            Join<PaymentSubmission, PaymentSubmissionBatch> batchJoin = root.join("batch", JoinType.INNER);
            return cb.equal(batchJoin.get("id"), batchId);
        };
    }

    public static Specification<PaymentSubmission> status(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<PaymentSubmission> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("paidAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("paidAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("paidAt"), endDate);
            }
        };
    }

    public static Specification<PaymentSubmission> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            query.distinct(true);

            String likePattern = "%" + keyword.toLowerCase() + "%";

            Join<PaymentSubmission, User> userJoin = root.join("checkedBy", JoinType.LEFT);
            Join<PaymentSubmission, Order> orderJoin = root.join("order", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), likePattern), // mã đối soát
                    cb.like(cb.lower(root.get("notes")), likePattern), // ghi chú
                    cb.like(
                            cb.lower(
                                    cb.concat(
                                            userJoin.get("lastName"),
                                            cb.concat(" ", userJoin.get("firstName")))),
                            likePattern), // người đối soát
                    cb.like(cb.lower(userJoin.get("phoneNumber")), likePattern), // SĐT
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern) // mã vận đơn
            );
        };
    }

}