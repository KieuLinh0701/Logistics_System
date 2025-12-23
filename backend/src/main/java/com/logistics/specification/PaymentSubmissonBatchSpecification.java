package com.logistics.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class PaymentSubmissonBatchSpecification {

    public static Specification<PaymentSubmissionBatch> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<PaymentSubmissionBatch> officeId(Integer officeId) {
        return (root, query, cb) -> {
            if (officeId == null)
                return null;
            return cb.equal(root.get("office").get("id"), officeId);
        };
    }

    public static Specification<PaymentSubmissionBatch> status(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<PaymentSubmissionBatch> createdAtBetween(LocalDateTime startDate,
            LocalDateTime endDate) {
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

    public static Specification<PaymentSubmissionBatch> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            query.distinct(true);

            String likePattern = "%" + keyword.toLowerCase() + "%";

            // Người nộp tiền
            Join<PaymentSubmissionBatch, User> shipperJoin = root.join("shipper", JoinType.LEFT);

            // Người kiểm tra
            Join<PaymentSubmissionBatch, User> checkerJoin = root.join("checkedBy", JoinType.LEFT);

            // Đơn hàng
            Join<PaymentSubmissionBatch, PaymentSubmission> submissionJoin = root.join("submissions", JoinType.LEFT);
            Join<PaymentSubmission, Order> orderJoin = submissionJoin.join("order", JoinType.LEFT);

            return cb.or(
                    // Mã đối soát batch
                    cb.like(cb.lower(root.get("code")), likePattern),

                    // Ghi chú
                    cb.like(cb.lower(root.get("notes")), likePattern),

                    // Người nộp (shipper)
                    cb.like(cb.lower(
                            cb.concat(
                                    shipperJoin.get("lastName"),
                                    cb.concat(" ", shipperJoin.get("firstName")))),
                            likePattern),
                    cb.like(cb.lower(shipperJoin.get("phoneNumber")), likePattern),

                    // Người kiểm tra
                    cb.like(cb.lower(
                            cb.concat(
                                    checkerJoin.get("lastName"),
                                    cb.concat(" ", checkerJoin.get("firstName")))),
                            likePattern),
                    cb.like(cb.lower(checkerJoin.get("phoneNumber")), likePattern),

                    // Mã đơn hàng
                    cb.like(cb.lower(orderJoin.get("trackingNumber")), likePattern));
        };
    }

}