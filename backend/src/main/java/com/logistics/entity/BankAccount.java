package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người sở hữu
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bankaccount_user"))
    private User user; // ID của user

    // ------------------- Thông tin tài khoản -------------------
    @Column(length = 100, nullable = false, columnDefinition = "NVARCHAR(100)")
    private String bankName; // Tên ngân hàng

    @Column(length = 50, nullable = false)
    private String accountNumber; // Số tài khoản

    @Column(length = 100, nullable = false, columnDefinition = "NVARCHAR(100)")
    private String accountName; // Tên chủ tài khoản

    @Column(nullable = false)
    private Boolean isDefault = false; // Có phải tài khoản mặc định không

    @Column(length = 500, nullable = false, columnDefinition = "NVARCHAR(500)")
    private String notes; // Ghi chú thêm

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}