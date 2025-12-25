package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerPaymentSubmissionBatchStatsDto;
import com.logistics.entity.PaymentSubmissionBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSubmissionBatchRepository
                extends JpaRepository<PaymentSubmissionBatch, Integer>,
                JpaSpecificationExecutor<PaymentSubmissionBatch> {

        // Thống kê tổng quan theo officeId
        @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerPaymentSubmissionBatchStatsDto(" +
                        "COUNT(b), " +
                        "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.PENDING THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.CHECKING THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.COMPLETED THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.PARTIAL THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.CANCELLED THEN 1 ELSE 0 END), 0)) "
                        +
                        "FROM PaymentSubmissionBatch b " +
                        "WHERE b.office.id = :officeId")
        ManagerPaymentSubmissionBatchStatsDto getPaymentSubmissionBatchStatsByOffice(
                        @Param("officeId") Integer officeId);
}