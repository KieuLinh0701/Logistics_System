package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.FeeConfiguration;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, Integer>, JpaSpecificationExecutor<FeeConfiguration> {
    List<FeeConfiguration> findByServiceTypeId(Integer serviceTypeId);
    List<FeeConfiguration> findByActive(Boolean active);

    // Lấy danh sách phí theo serviceType và active = true
    @Query("SELECT f FROM FeeConfiguration f WHERE (f.serviceType.id = :serviceTypeId OR f.serviceType IS NULL) AND f.active = true")
    List<FeeConfiguration> findActiveByServiceTypeIdIncludingNull(@Param("serviceTypeId") Integer serviceTypeId);

}
