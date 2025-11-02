package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.ShippingRequestAttachment.ShippingRequestAttachmentType;

@Entity
@Table(name = "shipping_request_attachments")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShippingRequestAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ nhiều file/ảnh thuộc về 1 ShippingRequest
    @ManyToOne
    @JoinColumn(name = "shipping_request_id", nullable = false)
    private ShippingRequest shippingRequest;

    @Column(length = 255, nullable = false)
    private String fileName;

    @Column(length = 500, nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingRequestAttachmentType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}