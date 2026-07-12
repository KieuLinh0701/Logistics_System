package com.logistics.repository;

import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettlementTransactionRepository
        extends JpaRepository<SettlementTransaction, Integer>, JpaSpecificationExecutor<SettlementTransaction> {

    List<SettlementTransaction> findBySettlementBatchId(Integer settlementBatchId, Sort sort);

    List<SettlementTransaction> findAllByCodeIn(List<String> codes);

    List<SettlementTransaction> findByStatusAndTypeAndCreatedAtBefore(
            SettlementTransactionStatus status,
            SettlementTransactionType type,
            LocalDateTime createdAt);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM SettlementTransaction t
            WHERE t.settlementBatch.shop.id = :userId
              AND t.type = com.logistics.enums.SettlementTransactionType.SYSTEM_TO_SHOP
              AND t.status = com.logistics.enums.SettlementTransactionStatus.SUCCESS
        """)
    BigDecimal sumActualPaidAmountByUser(@Param("userId") Integer userId);

    @Modifying
    @Query("""
        UPDATE SettlementTransaction t
        SET t.status = :newStatus, t.paidAt = :now, t.referenceCode = :refCode
        WHERE t.id = :id AND t.status = com.logistics.enums.SettlementTransactionStatus.PENDING
    """)
    int markProcessedIfPending(@Param("id") Integer id,
                               @Param("newStatus") SettlementTransactionStatus newStatus,
                               @Param("now") LocalDateTime now,
                               @Param("refCode") String refCode);
}