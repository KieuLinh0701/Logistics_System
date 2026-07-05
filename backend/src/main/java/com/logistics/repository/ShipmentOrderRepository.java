package com.logistics.repository;

import com.logistics.entity.Shipment;
import com.logistics.entity.ShipmentOrder;
import com.logistics.entity.id.ShipmentOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentOrderRepository extends JpaRepository<ShipmentOrder, ShipmentOrderId> {
    List<ShipmentOrder> findByShipmentId(Integer shipmentId);
    List<ShipmentOrder> findByOrderId(Integer orderId);
    boolean existsByShipmentIdAndOrderId(Integer shipmentId, Integer orderId);

   // Trả về danh sách ShipmentOrder theo shipmentId
    @Query("""
                SELECT so FROM ShipmentOrder so
                WHERE so.shipment.id = :shipmentId
                ORDER BY CASE WHEN so.stopSequence IS NULL THEN 1 ELSE 0 END ASC,
                         so.stopSequence ASC,
                         so.id.orderId ASC
            """)
    List<ShipmentOrder> findByShipmentIdOrderByStopSequenceAsc(@Param("shipmentId") Integer shipmentId);

    // Trả về stopSequence lớn nhất trong shipment (dùng để append stop mới ở cuối)
    @Query("""
                SELECT MAX(so.stopSequence) FROM ShipmentOrder so
                WHERE so.shipment.id = :shipmentId
            """)
    Integer findMaxStopSequenceByShipmentId(@Param("shipmentId") Integer shipmentId);

    @Query("""
                SELECT s FROM Shipment s
                JOIN ShipmentOrder so ON so.shipment.id = s.id
                WHERE so.order.id = :orderId
                  AND s.status IN (
                      com.logistics.enums.ShipmentStatus.PENDING,
                      com.logistics.enums.ShipmentStatus.IN_TRANSIT)
                ORDER BY s.createdAt DESC
            """)
    List<Shipment> findActiveShipmentsForOrder(@Param("orderId") Integer orderId);

    // Trả về danh sách orderId của các đơn đang được giao (DELIVERY) bởi shipper (employeeId) với trạng thái PENDING hoặc IN_TRANSIT  
    @Query("""
                SELECT DISTINCT so.order.id FROM ShipmentOrder so
                WHERE so.shipment.employee.id = :employeeId
                  AND so.shipment.type = com.logistics.enums.ShipmentType.DELIVERY
                  AND so.shipment.status IN (
                      com.logistics.enums.ShipmentStatus.PENDING,
                      com.logistics.enums.ShipmentStatus.IN_TRANSIT)
                  AND so.stopType = com.logistics.enums.RouteStopType.DELIVERY
            """)
    List<Integer> findOrderIdsByActiveDeliveryShipmentOfEmployee(@Param("employeeId") Integer employeeId);
}
