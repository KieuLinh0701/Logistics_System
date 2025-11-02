package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.Product.ProductStatus;
import com.logistics.enums.Product.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products", 
       uniqueConstraints = @UniqueConstraint(name = "unique_product_per_user", columnNames = {"user_id", "name"}))
@Data
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 20, nullable = true, unique = true)
    private String code; // Thêm này cho mã sản phẩm (PROD_SỐ SẢN PHẨM HIỆN TẠI NGƯỜI DÙNG + 1)

    // ------------------- Người bán / chủ sở hữu -------------------
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ------------------- Thông tin sản phẩm -------------------
    @Column(length = 255)
    private String image;

    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal weight;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Integer soldQuantity = 0;

    // ------------------- Quan hệ với OrderProduct -------------------
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts;

    // ------------------- Quan hệ nhiều-nhiều với Order -------------------
    @ManyToMany(mappedBy = "products")
    private Set<Order> orders;

    // ------------------- Thời gian tạo / cập nhật -------------------
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}