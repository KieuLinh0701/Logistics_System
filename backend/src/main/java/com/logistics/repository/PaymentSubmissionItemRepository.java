package com.logistics.repository;

import com.logistics.entity.PaymentSubmissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSubmissionItemRepository extends JpaRepository<PaymentSubmissionItem, Integer> {
    List<PaymentSubmissionItem> findByPaymentSubmission_Id(Integer submissionId);
}
