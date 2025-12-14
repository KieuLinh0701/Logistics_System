package com.logistics.repository;

import com.logistics.entity.ShipmentOrder;
import com.logistics.id.ShipmentOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentOrderRepository extends JpaRepository<ShipmentOrder, ShipmentOrderId> {
    List<ShipmentOrder> findByShipmentId(Integer shipmentId);
    List<ShipmentOrder> findByOrderId(Integer orderId);
    boolean existsByShipmentIdAndOrderId(Integer shipmentId, Integer orderId);
}
