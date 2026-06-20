package com.logistics.entity;

import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
@EntityListeners(AuditingEntityListener.class)
public class PickupAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PickupAttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PickupFailReason failReason;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String note;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptedAt;
}
