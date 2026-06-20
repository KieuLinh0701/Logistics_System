package com.logistics.entity;

import com.logistics.enums.OrderHistoryActionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "order_histories")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "fromOfficeId", nullable = true)
    private Office fromOffice;

    @ManyToOne
    @JoinColumn(name = "toOfficeId", nullable = true)
    private Office toOffice;

    @ManyToOne
    @JoinColumn(name = "shipmentId", nullable = true)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderHistoryActionType action;

    @Lob
    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime actionTime = LocalDateTime.now();
}