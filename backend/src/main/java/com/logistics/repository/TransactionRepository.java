package com.logistics.repository;

import com.logistics.entity.Transaction;
import com.logistics.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByCode(String code);
    List<Transaction> findByOrderId(Integer orderId);
    List<Transaction> findByCollectedById(Integer collectedById);
    List<Transaction> findByType(TransactionType type);
}
