package com.logistics.repository;

import com.logistics.entity.SettlementBatch;
import com.logistics.entity.User;
import com.logistics.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementBatchRepository
    extends JpaRepository<SettlementBatch, Integer>, JpaSpecificationExecutor<SettlementBatch> {
  Optional<SettlementBatch> findByIdAndShop_Id(Integer id, Integer shopId);

  // Tổng tiền đã nhận: batch đã COMPLETED theo type SYSTEM_TO_SHOP
  @Query("""
          SELECT COALESCE(SUM(t.amount), 0)
          FROM SettlementTransaction t
          WHERE t.settlementBatch.shop.id = :userId
            AND t.status = com.logistics.enums.SettlementTransactionStatus.SUCCESS
            AND t.type = com.logistics.enums.SettlementTransactionType.SYSTEM_TO_SHOP
      """)
  BigDecimal sumReceivedByUser(@Param("userId") Integer userId);

  // Lấy các batch PENDING sắp tới
  @Query("""
          SELECT b
          FROM SettlementBatch b
          WHERE b.shop.id = :userId
            AND b.status = com.logistics.enums.SettlementStatus.PENDING
          ORDER BY b.createdAt ASC
      """)
  List<SettlementBatch> findNextPendingByUser(@Param("userId") Integer userId);

  // Tính nợ còn lại theo type SYSTEM_TO_SHOP chưa completed
  @Query("""
          SELECT COALESCE(SUM(b.balanceAmount), 0)
          FROM SettlementBatch b
          WHERE b.shop.id = :userId
            AND b.status IN (com.logistics.enums.SettlementStatus.PENDING, com.logistics.enums.SettlementStatus.PARTIAL)
      """)
  BigDecimal sumPendingDebtByUser(@Param("userId") Integer userId);

  // Lấy các đối soát cần xét nợ
  @Query("""
          SELECT b
          FROM SettlementBatch b
          WHERE b.shop.id = :userId
            AND b.balanceAmount < 0
            AND b.status IN (
              com.logistics.enums.SettlementStatus.PENDING,
              com.logistics.enums.SettlementStatus.PARTIAL,
              com.logistics.enums.SettlementStatus.FAILED
            )
      """)
  List<SettlementBatch> findDebtBatchesByUser(Integer userId);

  List<SettlementBatch> findByStatusInAndCreatedAtBefore(
      List<SettlementStatus> statuses,
      LocalDateTime time);

  boolean existsByShopAndStatusIn(User shop, List<SettlementStatus> statuses);

  List<SettlementBatch> findByStatusIn(List<SettlementStatus> statuses);

  List<SettlementBatch> findAllByIdInAndShop_Id(List<Integer> ids, Integer shopId);

  List<SettlementBatch> findByShopAndStatusIn(User shop, List<SettlementStatus> statuses);
}