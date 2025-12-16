package com.logistics.repository;

import com.logistics.entity.PaymentSubmission;
import com.logistics.enums.PaymentSubmissionStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSubmissionRepository
        extends JpaRepository<PaymentSubmission, Integer>, JpaSpecificationExecutor<PaymentSubmission> {
    List<PaymentSubmission> findByBatchIsNullAndStatusIn(List<PaymentSubmissionStatus> statuses);
}