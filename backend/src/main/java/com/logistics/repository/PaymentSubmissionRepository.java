package com.logistics.repository;

import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.enums.PaymentSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentSubmissionRepository extends JpaRepository<PaymentSubmission, Integer>, JpaSpecificationExecutor<PaymentSubmission> {
    Optional<PaymentSubmission> findByCode(String code);
    List<PaymentSubmission> findByOrderId(Integer orderId);
    @Query("SELECT DISTINCT p FROM PaymentSubmission p LEFT JOIN FETCH p.items it WHERE p.order.id = :orderId")
    List<PaymentSubmission> findByOrderIdWithItems(@Param("orderId") Integer orderId);
    List<PaymentSubmission> findByStatus(PaymentSubmissionStatus status);
    
    List<PaymentSubmission> findByBatchIsNullAndStatusIn(List<PaymentSubmissionStatus> statuses);

    List<PaymentSubmission> findByShipperIdAndStatusIn(Integer shipperId, List<PaymentSubmissionStatus> statuses);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentSubmission p WHERE p.id IN :ids")
    List<PaymentSubmission> findByIdInForUpdate(@Param("ids") List<Integer> ids);

    Optional<PaymentSubmission> findTopByOrderAndStatusInOrderByPaidAtDesc(Order order, List<PaymentSubmissionStatus> statuses);

        @Query("SELECT COALESCE(SUM(p.actualAmount), 0) FROM PaymentSubmission p WHERE p.order.id = :orderId AND p.status IN :statuses")
        BigDecimal sumActualAmountByOrderIdAndStatusIn(@Param("orderId") Integer orderId,
                @Param("statuses") List<PaymentSubmissionStatus> statuses);

}

