package com.logistics.repository;

import com.logistics.entity.PaymentSubmissionBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSubmissionBatchRepository
        extends JpaRepository<PaymentSubmissionBatch, Integer>, JpaSpecificationExecutor<PaymentSubmissionBatch> {
}