package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.SettlementStatus;

@Entity
@Table(name = "settlement_batches")
@Data
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // VD: SB_20251216_001

    // Shop đối soát
    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private User shop;

    // Thực tế shop nhận được (- thì shop nợ hệ thống, dương hệ thống nợ shop)
    @Column
    private BigDecimal balanceAmount;

    // Trạng thái đối soát
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "settlementBatch")
    private List<Order> orders;

    @PostPersist
    private void generateCode() {
        if (this.code == null) {
            String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            this.code = "SB" + date + this.id;
        }
    }
}