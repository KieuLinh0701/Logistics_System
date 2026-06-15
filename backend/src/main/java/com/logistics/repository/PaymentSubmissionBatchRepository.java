package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerPaymentSubmissionBatchStatsDto;
import com.logistics.entity.PaymentSubmissionBatch;

import com.logistics.enums.PaymentSubmissionBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentSubmissionBatchRepository
        extends JpaRepository<PaymentSubmissionBatch, Integer>,
        JpaSpecificationExecutor<PaymentSubmissionBatch> {

    // Thống kê tổng quan theo officeId
    @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerPaymentSubmissionBatchStatsDto(" +
            "COUNT(b), " +
            "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.OPEN THEN 1 ELSE "
            + "0 END), 0), "
            +
            "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.PROCESSING THEN 1 ELSE 0 "
            + "END), 0), "
            +
            "COALESCE(SUM(CASE WHEN b.status = com.logistics.enums.PaymentSubmissionBatchStatus.COMPLETED THEN 1 ELSE 0 END), 0)) "
            +
            "FROM PaymentSubmissionBatch b " +
            "WHERE b.office.id = :officeId")
    ManagerPaymentSubmissionBatchStatsDto getPaymentSubmissionBatchStatsByOffice(
            @Param("officeId") Integer officeId);

    Optional<PaymentSubmissionBatch> findByShipperIdAndStatus(
            Integer shipperId,
            PaymentSubmissionBatchStatus status);
}