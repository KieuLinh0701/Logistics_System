package com.logistics.entity;

import com.logistics.enums.AiRoutePlanStatus;
import jakarta.persistence.*;
import lombok.*;
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

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiRoutePlanRoute> routes = new ArrayList<>();
}
