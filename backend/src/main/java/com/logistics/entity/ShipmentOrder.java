package com.logistics.entity;

import com.logistics.entity.id.ShipmentOrderId;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipment_orders")
@EntityListeners(AuditingEntityListener.class)
public class ShipmentOrder {

    @EmbeddedId
    private ShipmentOrderId id;

    @ManyToOne
    @MapsId("shipmentId") 
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}