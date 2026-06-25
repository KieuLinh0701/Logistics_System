package com.logistics.entity;

import com.logistics.enums.RouteMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_route_plan_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiRoutePlanRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private AiRoutePlan plan;

    @Column(name = "shipper_user_id", nullable = false)
    private Integer shipperUserId;

    @Column(name = "shipper_employee_id", nullable = false)
    private Integer shipperEmployeeId;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String shipperName;

    private Integer routeSequence = 0;
    private BigDecimal estimatedDistanceKm;
    private BigDecimal estimatedDurationMinutes;
    private BigDecimal fuelCost;
    private Long totalCod;

    @Column(columnDefinition = "TEXT")
    private String encodedPolyline;

    @Column(length = 10)
    private String startTime = "08:00";

    private Integer stopCount = 0;

    @Column(name = "route_version")
    private Integer routeVersion = 1;

    @Column(name = "parent_route_id")
    private Long parentRouteId;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RouteMode routeMode = RouteMode.CLOSED_LOOP;

    @Column(name = "return_to_office")
    private Boolean returnToOffice = true;

    @Column(name = "actual_started_at")
    private LocalDateTime actualStartedAt;

    @Column(name = "actual_completed_at")
    private LocalDateTime actualCompletedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "reoptimized_at")
    private LocalDateTime reoptimizedAt;

    @Column(length = 50)
    private String reoptimizeReason;

    @Column(name = "shipment_id")
    private Integer shipmentId;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopSequence ASC")
    private List<AiRoutePlanStop> stops = new ArrayList<>();
}
