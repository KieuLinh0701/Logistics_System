package com.logistics.repository;

import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

  Optional<SettlementTransaction> findByCode(String code);

  // Tổng tiền đã trả thành công
  @Query("""
          SELECT COALESCE(SUM(t.amount), 0)
          FROM SettlementTransaction t
          WHERE t.settlementBatch.id = :batchId
            AND t.status = com.logistics.enums.SettlementTransactionStatus.SUCCESS
            AND t.type = com.logistics.enums.SettlementTransactionType.SHOP_TO_SYSTEM
      """)
  BigDecimal sumPaidDebtByBatch(Integer batchId);

  List<SettlementTransaction> findAllByCodeIn(List<String> codes);

}