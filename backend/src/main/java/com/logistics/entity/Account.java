package com.logistics.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Audited
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountRole> accountRoles;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}