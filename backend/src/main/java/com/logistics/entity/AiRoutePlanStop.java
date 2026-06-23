package com.logistics.entity;

import com.logistics.enums.RouteStopStatus;
import com.logistics.enums.RouteStopType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStopType stopType = RouteStopType.DELIVERY;

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

    private BigDecimal legDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RouteStopStatus stopStatus = RouteStopStatus.PENDING;

    @Column(name = "is_inserted")
    private Boolean isInserted = false;

    @Column(length = 100)
    private String insertedReason;

    @Column(name = "original_sequence")
    private Integer originalSequence;

    @Column(name = "actual_arrived_at")
    private LocalDateTime actualArrivedAt;

    @Column(name = "actual_completed_at")
    private LocalDateTime actualCompletedAt;

    @Column(name = "service_time_minutes")
    private Integer serviceTimeMinutes;

    @Column(name = "leg_duration_minutes")
    private Integer legDurationMinutes;
}
