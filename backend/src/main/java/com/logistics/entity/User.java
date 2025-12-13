package com.logistics.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã báo cáo (USER_id)

    // Liên kết với Account
    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @Column(columnDefinition = "NVARCHAR(50)", nullable = false)
    private String firstName;

    @Column(columnDefinition = "NVARCHAR(50)", nullable = false)
    private String lastName;

    @Transient
    public String getFullName() {
        return lastName + " " + firstName;
    }

    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @Column(length = 255)
    private String images; // Lưu đường dẫn ảnh

    // Quan hệ với Employee
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Employee> employees = new ArrayList<>();

    // Quan hệ với Product
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval =
    true)
    private List<Product> products;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PostPersist
    private void generateCode() {
        this.code = "USER" + id;
    }
} 