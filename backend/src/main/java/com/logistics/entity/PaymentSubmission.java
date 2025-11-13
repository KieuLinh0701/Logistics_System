// Chỉ đối soát cho các đơn thu tiền COD + TOTALFEE (NẾU CÓ)
package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.PaymentSubmissionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "payment_submissions")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PaymentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã đối soát (COD_NGÀY THÁNG NĂM TẠO_ID)

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private BigDecimal systemAmount; 

    @Column(nullable = false)
    private BigDecimal actualAmount; 

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentSubmissionStatus status; 

    @ManyToOne
    @JoinColumn(name = "checked_by") 
    private User checkedBy;

    private LocalDateTime checkedAt;

    @Column(length = 255)
    private String notes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime paidAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}