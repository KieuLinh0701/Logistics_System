package com.logistics.repository;

import com.logistics.entity.SettlementBatch;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementBatchRepository
                extends JpaRepository<SettlementBatch, Integer>, JpaSpecificationExecutor<SettlementBatch> {
        Optional<SettlementBatch> findByIdAndShop_Id(Integer id, Integer shopId);
}