package com.logistics.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.SupportTicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "support_tickets")
@EntityListeners(AuditingEntityListener.class)
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "created_by_account_id", nullable = false)
    private Integer createdByAccountId;

    @Column(name = "assigned_to_account_id")
    private Integer assignedToAccountId;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Integer relatedId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "office_id")
    private Integer officeId;

    @Column(length = 255)
    private String subject;

    @Column(length = 20)
    private String priority = "NORMAL";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by_account_id")
    private Integer closedByAccountId;

    @Column(name = "closed_by_name")
    private String closedByName;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
