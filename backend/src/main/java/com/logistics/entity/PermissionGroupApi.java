package com.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "permission_group_apis")
@Data
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PermissionGroupApi {

    @EmbeddedId
    private PermissionGroupApiId id = new PermissionGroupApiId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private PermissionGroup permissionGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("apiId")
    @JoinColumn(name = "api_id", nullable = false)
    private PermissionApi permissionApi;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Composite Key class
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionGroupApiId implements java.io.Serializable {

        @Column(name = "group_id")
        private Integer groupId;

        @Column(name = "api_id")
        private Integer apiId;
    }
}