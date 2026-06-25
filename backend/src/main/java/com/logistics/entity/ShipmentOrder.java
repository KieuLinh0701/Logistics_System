package com.logistics.entity;

import com.logistics.entity.id.ShipmentOrderId;
import com.logistics.enums.RouteStopType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipment_orders")
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"shipment", "order"})
@EqualsAndHashCode(exclude = {"shipment", "order"})
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

    // ==================== Phase 3A: stop sequence + ETA snapshot ====================
    // These fields make ShipmentOrder the source of truth for stop order.
    // They are snapshot from AiRoutePlanStop at confirm-plan time and updated
    // in-place by re-optimize (Phase 3B) and pickup-insert (Phase 3C).

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", length = 20)
    private RouteStopType stopType = RouteStopType.DELIVERY;

    @Column(name = "eta_time", length = 10)
    private String etaTime;

    @Column(name = "eta_minutes_from_start")
    private Integer etaMinutesFromStart;

    @Column(name = "leg_distance_km", precision = 10, scale = 2)
    private BigDecimal legDistanceKm;

    @Column(name = "leg_duration_minutes")
    private Integer legDurationMinutes;
}
