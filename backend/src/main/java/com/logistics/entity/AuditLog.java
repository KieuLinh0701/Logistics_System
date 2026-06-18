package com.logistics.entity;

import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.enums.AuditLogStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id",      columnList = "user_id"),
        @Index(name = "idx_audit_office_id",      columnList = "office_id"),
        @Index(name = "idx_audit_shop_id",      columnList = "shop_id"),
        @Index(name = "idx_audit_user_id",      columnList = "user_id"),
        @Index(name = "idx_audit_entity",        columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created_at",    columnList = "created_at"),
        @Index(name = "idx_audit_module_action", columnList = "module, action")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người thực hiện
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nhân viên thuộc bưu cục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private User shop;

    // Tác động lên entity nào
    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entity;

    // id của record bị tác động
    @Column(length = 50)
    private String entityId;

    // code của record đó
    @Column(length = 100)
    private String entityCode;

    // Hành động thực hiện
    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditLogAction action;

    // Nội dung trước khi thay đổi
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String payloadBefore;

    // Nội dung sau khi thay đổi
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String payloadAfter;

    // Mô tả ngắn hiển thị trên UI
    @Column(columnDefinition = "NVARCHAR(500)")
    private String description;

    // Kết quả hành động
    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuditLogStatus status = AuditLogStatus.SUCCESS;

    // Lưu
    @Column(columnDefinition = "NVARCHAR(500)")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}