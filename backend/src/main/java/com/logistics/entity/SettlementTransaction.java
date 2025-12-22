package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;

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
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settlement_transactions")
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SettlementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true)
    private String code; // ST_20251216_001

    @ManyToOne
    @JoinColumn(name = "settlement_batch_id", nullable = false)
    private SettlementBatch settlementBatch;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementTransactionStatus status;

    // Ngân hàng chuyển vào
    @Column(length = 100, nullable = true, columnDefinition = "NVARCHAR(100)")
    private String bankName; // Tên ngân hàng

    @Column(length = 50, nullable = true)
    private String accountNumber; // Số tài khoản

    @Column(length = 100, nullable = true, columnDefinition = "NVARCHAR(100)")
    private String accountName; // Tên chủ tài khoản

    @Column(length = 255, nullable = true)
    private String referenceCode; // Mã giao dịch

    private LocalDateTime paidAt;

    @PostPersist
    private void generateCode() {
        if (this.code == null) {
            String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            this.code = "ST" + date + this.id;
        }
    }

}