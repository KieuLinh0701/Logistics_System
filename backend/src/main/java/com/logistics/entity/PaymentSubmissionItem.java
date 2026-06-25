package com.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_submission_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSubmissionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_submission_id", nullable = false)
    private PaymentSubmission paymentSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id", nullable = false)
    private OrderProduct orderProduct;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private BigDecimal unitAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;
}
