package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.IncidentPriority;
import com.logistics.enums.IncidentStatus;
import com.logistics.enums.IncidentType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Audited
@Setter
@Getter
@Table(name = "incident_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã báo cáo (IR_NGÀY THÁNG NĂM TẠO_officeId_id)

    // Quan hệ với đơn hàng
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_incident_order"))
    private Order order;

    // Shipper báo cáo sự cố
    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false, foreignKey = @ForeignKey(name = "fk_incident_shipper"))
    private User shipper;

    // Người xử lý báo cáo (có thể null)
    @ManyToOne
    @JoinColumn(name = "handled_by", nullable = true, foreignKey = @ForeignKey(name = "fk_incident_handler"))
    private User handler;

    // Loại sự cố
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private IncidentType incidentType;

    @Column(columnDefinition = "NVARCHAR(255)", length = 255, nullable = false)
    private String title;

    @Lob
    private String description;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private IncidentPriority priority = IncidentPriority.MEDIUM;

    @ElementCollection
    @CollectionTable(name = "incident_report_images", joinColumns = @JoinColumn(name = "incident_report_id"))
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private List<String> images;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private IncidentStatus status = IncidentStatus.PENDING;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String resolution;

    private LocalDateTime handledAt;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PostPersist
    private void generateCode() {
        if (this.code == null) {
            String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            this.code = "IR" + date + office.getId() + this.id;
        }
    }
}   