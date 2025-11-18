package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;

@Repository
public interface ServiceTypeRepository
        extends JpaRepository<ServiceType, Integer>, JpaSpecificationExecutor<ServiceType> {
    List<ServiceType> findByStatus(ServiceTypeStatus status);

    @Query("SELECT DISTINCT s FROM ServiceType s LEFT JOIN FETCH s.rates WHERE s.status = :status")
    List<ServiceType> findAllWithRatesByStatus(ServiceTypeStatus status);

}
