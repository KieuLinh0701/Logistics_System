package com.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "permission_apis",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_url_method",
                columnNames = {"url", "method"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PermissionApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255, nullable = false)
    private String url; // /api/user/orders, /api/user/orders/*

    @Column(length = 10, nullable = false)
    private String method; // GET, POST, PUT, PATCH, DELETE

    @Column(length = 100, nullable = false)
    private String name; // Tên mô tả ngắn của API

    // TRUE = API chính, hiển thị trên UI để chủ shop tích chọn
    // FALSE = API bổ trợ, ẩn đi, chỉ đính kèm vào group
    @Column(nullable = false)
    private Boolean isUiSelectable = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "permissionApi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PermissionGroupApi> permissionGroupApis = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}