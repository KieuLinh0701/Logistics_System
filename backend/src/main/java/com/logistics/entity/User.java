package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"account", "addresses", "employees", "products", "shipperAssignments"})
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
    private List<Address> addresses = new ArrayList<>();;

    @Column(length = 255)
    private String images; // Lưu đường dẫn ảnh

    // Quan hệ với Employee
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Employee> employees = new ArrayList<>();

    // Quan hệ với Product
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();;

    @OneToMany(mappedBy = "shipper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipperAssignment> shipperAssignments = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_shop_id")
    private User currentShop;

    @OneToMany(mappedBy = "user")
    private List<ShopWorkHistory> shopWorkHistories = new ArrayList<>();

    @Column(nullable = false)
    private Boolean locked = false;

    @PostPersist
    private void generateCode() {
        if (this.code == null) {
            String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            this.code = "USER" + date + this.id;
        }
    }
}