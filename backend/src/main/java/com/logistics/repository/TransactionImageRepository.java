package com.logistics.repository;

import com.logistics.entity.TransactionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionImageRepository extends JpaRepository<TransactionImage, Integer> {
    List<TransactionImage> findByTransactionId(Integer transactionId);
}

