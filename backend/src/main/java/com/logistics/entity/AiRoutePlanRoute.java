package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("stopSequence ASC")
    private List<AiRoutePlanStop> stops = new ArrayList<>();
}
