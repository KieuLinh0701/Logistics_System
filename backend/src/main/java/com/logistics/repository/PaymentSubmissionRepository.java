package com.logistics.repository;

import com.logistics.entity.PaymentSubmission;
import com.logistics.enums.PaymentSubmissionStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentSubmissionRepository extends JpaRepository<PaymentSubmission, Integer>, JpaSpecificationExecutor<PaymentSubmission> {
    Optional<PaymentSubmission> findByCode(String code);
    List<PaymentSubmission> findByOrderId(Integer orderId);
    List<PaymentSubmission> findByStatus(PaymentSubmissionStatus status);
    
    List<PaymentSubmission> findByBatchIsNullAndStatusIn(List<PaymentSubmissionStatus> statuses);

    List<PaymentSubmission> findByShipperIdAndStatusIn(Integer shipperId, List<PaymentSubmissionStatus> statuses);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentSubmission p WHERE p.id IN :ids")
    List<PaymentSubmission> findByIdInForUpdate(@Param("ids") List<Integer> ids);

}

