package com.logistics.entity;

import com.logistics.enums.DeliveryAttemptStatus;
import com.logistics.enums.DeliveryFailReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {

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
    private DeliveryAttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private DeliveryFailReason failReason;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String note;

    @Column(name = "proof_image_url", length = 500)
    private String proofImageUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
