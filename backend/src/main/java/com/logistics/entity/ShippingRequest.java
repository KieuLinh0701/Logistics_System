package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;
import com.logistics.enums.ShippingRequestAttachmentType;

@Entity
@Table(name = "shipping_requests")
@Data
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShippingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã yêu cầu (SR_NGÀY THÁNG NĂM TẠO_id)

    // Quan hệ tới Order
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    // Quan hệ tới Office
    @ManyToOne
    @JoinColumn(name = "office_id", nullable = true)
    private Office office;

    // Người tạo yêu cầu (nếu có tài khoản)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // Người xử lý/ phản hồi yêu cầu
    @ManyToOne
    @JoinColumn(name = "handler_id", nullable = true)
    private User handler;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ShippingRequestType requestType;

    @Column(columnDefinition = "TEXT")
    private String requestContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingRequestStatus status = ShippingRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String response;

    // Thông tin liên hệ khi người dùng không có tài khoản
    @Column(columnDefinition = "NVARCHAR(100)")
    private String contactName;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhoneNumber;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    private LocalDateTime responseAt;

    // File/ảnh đính kèm
    @OneToMany(mappedBy = "shippingRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ShippingRequestAttachment> attachments;

    public List<ShippingRequestAttachment> getRequestAttachments() {
        return attachments.stream()
                .filter(a -> a.getType() == ShippingRequestAttachmentType.REQUEST)
                .toList();
    }

    public List<ShippingRequestAttachment> getResponseAttachments() {
        return attachments.stream()
                .filter(a -> a.getType() == ShippingRequestAttachmentType.RESPONSE)
                .toList();
    }

    // Thời gian tạo/cập nhật
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime paidAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PostPersist
    private void generateCode() {
        this.code = "SR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + id;
    }
}