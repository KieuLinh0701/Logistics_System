package com.logistics.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import lombok.*;

@Audited
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "role_id"})
})
public class AccountRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // cho biết nhân viên đó còn hoạt động không
}
