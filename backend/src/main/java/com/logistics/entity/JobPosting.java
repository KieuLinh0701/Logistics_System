package com.logistics.entity;

import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import com.logistics.enums.EmployeeShift;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_postings")
@EntityListeners(AuditingEntityListener.class)
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecruitmentRoleType roleType;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.OPEN;

    @Column(name = "quantity_needed")
    private Integer quantityNeeded;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20)
    private EmployeeShift shift;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Account createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
