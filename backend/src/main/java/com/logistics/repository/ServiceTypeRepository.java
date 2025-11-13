package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Integer> {
    List<ServiceType> findByStatus(ServiceTypeStatus status);
}
