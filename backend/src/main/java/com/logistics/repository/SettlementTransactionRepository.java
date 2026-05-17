package com.logistics.repository;

import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.logistics.enums.SettlementTransactionType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementTransactionRepository
        extends JpaRepository<SettlementTransaction, Integer>, JpaSpecificationExecutor<SettlementTransaction> {

    List<SettlementTransaction> findBySettlementBatchId(Integer settlementBatchId, Sort sort);

    List<SettlementTransaction> findAllByCodeIn(List<String> codes);

    List<SettlementTransaction> findByStatusAndTypeAndCreatedAtBefore(
            SettlementTransactionStatus status,
            SettlementTransactionType type,
            LocalDateTime createdAt);
}