package com.logistics.entity;

import com.logistics.enums.AiRoutePlanStatus;
import com.logistics.enums.RouteMode;
import com.logistics.enums.RouteOptimizationScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_route_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AiRoutePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_employee_id", nullable = false)
    private Employee managerEmployee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiRoutePlanStatus status = AiRoutePlanStatus.DRAFT;

    @Column(length = 50)
    private String planCode;

    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationMinutes;
    private BigDecimal totalFuelCost;
    private Long totalCod;
    private Integer unassignedCount = 0;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String optimizationNote;

    private LocalDateTime confirmedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RouteMode routeMode = RouteMode.CLOSED_LOOP;

    @Column(nullable = false)
    private Boolean returnToOffice = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RouteOptimizationScope optimizationScope = RouteOptimizationScope.MANAGER_GLOBAL;

    @Column(length = 20)
    private String createdByRole = "MANAGER";

    @Column(name = "created_by_employee_id")
    private Integer createdByEmployeeId;

    @Column(name = "base_plan_id")
    private Long basePlanId;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "parent_plan_id")
    private Long parentPlanId;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiRoutePlanRoute> routes = new ArrayList<>();
}
