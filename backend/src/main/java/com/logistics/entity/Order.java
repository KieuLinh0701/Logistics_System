package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.OrderStatus;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.OrderPickupType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "orders")
@NoArgsConstructor
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Order {

    // Id đơn hàng
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mã vận đơn
    @Column(length = 50, unique = true, nullable = true)
    private String trackingNumber; // Mã vận đơn kiểu UTE_11 chữ số khác

    // Trạng thái
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // ------------------- Người tạo đơn -------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrderCreatorType createdByType;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user; // Người sở hữu đơn hàng

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee; // Nhân viên tạo đơn hàng nếu như tạo tại bưu cục

    // ------------------- Người gửi -------------------
    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String senderPhone;

    @Column(nullable = false)
    private Integer senderCityCode;

    @Column(nullable = false)
    private Integer senderWardCode;

    @Column(nullable = false)
    private String senderDetail;

    @ManyToOne
    @JoinColumn(name = "sender_address_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Address senderAddress;

    // ------------------- Người nhận -------------------
    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @ManyToOne
    @JoinColumn(name = "recipient_address_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Address recipientAddress;

    // ------------------- Hình thức lấy hàng -------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private OrderPickupType pickupType = OrderPickupType.PICKUP_BY_COURIER;

    // ------------------- Thông tin đơn hàng -------------------
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal weight;

    @ManyToOne
    @JoinColumn(name = "service_type_id", nullable = false)
    @NotAudited
    private ServiceType serviceType;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    @NotAudited
    private Promotion promotion;

    @Column(nullable = false)
    private Integer discountAmount = 0;

    @Column(nullable = false)
    private Integer shippingFee;

    @Column(nullable = false)
    private Integer cod = 0;

    @Column(nullable = false)
    private Integer orderValue = 0;

    @Column(nullable = false)
    private Integer totalFee = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrderPayerType payer = OrderPayerType.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrderPaymentStatus paymentStatus = OrderPaymentStatus.UNPAID;

    // ------------------- Thông tin khác -------------------
    @Column(columnDefinition = "NVARCHAR(1000)")
    private String notes;

    private LocalDateTime deliveredAt;

    @ManyToOne
    @JoinColumn(name = "from_office_id")
    private Office fromOffice;

    @ManyToOne
    @JoinColumn(name = "to_office_id")
    private Office toOffice;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderCodStatus codStatus;

    @OneToOne(mappedBy = "order")
    private PaymentSubmission paymentSubmission;
}