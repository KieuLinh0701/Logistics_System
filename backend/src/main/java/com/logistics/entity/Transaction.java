package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.Transaction.TransactionStatus;
import com.logistics.enums.Transaction.TransactionMethod;
import com.logistics.enums.Transaction.TransactionPurpose;
import com.logistics.enums.Transaction.TransactionType;

@Entity
@Audited
@Table(name = "transactions")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã báo cáo (TRANS_NGÀY THÁNG NĂM TẠO_id)

    // Liên kết đơn hàng
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Nhân viên thu tiền (COD)
    @ManyToOne
    @JoinColumn(name = "collected_by", nullable = true)
    private User collectedBy;

    // Tiêu đề giao dịch
    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String title;

    // Số tiền
    @Column(nullable = false)
    private Integer amount;

    // Loại giao dịch
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private TransactionType type; 

    // Phương thức
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionMethod method;

    // Mục đích giao dịch
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TransactionPurpose purpose;

    // Trạng thái xác nhận
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    // Ghi chú thêm
    @Column(length = 255, nullable = true)
    private String notes;

    // Hình ảnh liên quan giao dịch
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<TransactionImage> images;

    // Thời gian tạo/cập nhật
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime paidAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}