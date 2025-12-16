package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.PaymentSubmissionBatchStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_submission_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Audited
@EntityListeners(AuditingEntityListener.class)
public class PaymentSubmissionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã đối soát (PSB_NGÀY THÁNG NĂM TẠO_ID)

    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private BigDecimal totalSystemAmount; // Tổng COD + TOTALFEE

    @Column
    private BigDecimal totalActualAmount; // Tổng tiền Shipper nộp

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentSubmissionBatchStatus status;

    @ManyToOne
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    private LocalDateTime checkedAt;

    @OneToMany(mappedBy = "batch")
    private List<PaymentSubmission> submissions; // Danh sách đơn trong phiên

    @Column(length = 255)
    private String notes;

    @PostPersist
    private void generateCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        this.code = "PSB" + date + id;
    }

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;
}