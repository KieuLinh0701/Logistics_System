package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.logistics.entity.FeeConfiguration;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, Integer>, JpaSpecificationExecutor<FeeConfiguration> {
    List<FeeConfiguration> findByServiceTypeId(Integer serviceTypeId);
    List<FeeConfiguration> findByActive(Boolean active);
}


