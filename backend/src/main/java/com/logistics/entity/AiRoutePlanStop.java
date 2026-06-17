package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_route_plan_stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiRoutePlanStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private AiRoutePlanRoute route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Integer stopSequence;

    @Column(length = 50)
    private String trackingNumber;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String recipientName;

    @Column(length = 50)
    private String recipientPhone;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String recipientAddress;

    private Double recipientLatitude;
    private Double recipientLongitude;

    private Integer codAmount = 0;

    @Column(length = 20)
    private String priority = "NORMAL";

    @Column(length = 10)
    private String etaTime;

    private Integer etaMinutesFromStart;

    private java.math.BigDecimal legDistanceKm;
}
