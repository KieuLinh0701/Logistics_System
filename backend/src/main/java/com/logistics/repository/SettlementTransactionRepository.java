package com.logistics.repository;

import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementTransactionRepository
                extends JpaRepository<SettlementTransaction, Integer>, JpaSpecificationExecutor<SettlementTransaction> {
        @Query("""
                            SELECT COALESCE(SUM(t.amount), 0)
                            FROM SettlementTransaction t
                            WHERE t.settlementBatch.id = :batchId
                              AND t.status = :status
                        """)
        BigDecimal sumAmountByBatchAndStatus(
                        Integer batchId,
                        SettlementTransactionStatus status);

        List<SettlementTransaction> findBySettlementBatchId(Integer settlementBatchId, Sort sort);
}