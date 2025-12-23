package com.logistics.repository;

import com.logistics.entity.ShippingRate;
import com.logistics.enums.ShippingRateRegionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Integer>,
        JpaSpecificationExecutor<ShippingRate> {
    List<ShippingRate> findByServiceType_IdAndRegionTypeOrderByWeightFromAsc(Integer serviceTypeId,
            ShippingRateRegionType regionType);
}