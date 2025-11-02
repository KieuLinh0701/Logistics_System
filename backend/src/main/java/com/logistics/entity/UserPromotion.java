// Bảng này dùng để tạo promotion riêng cho từng khách hàng hoặc giới hạn số lượt sử dụng của mỗi khách hàng
package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_promotions")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nếu null -> áp dụng cho tất cả user, nếu có -> áp dụng riêng cho user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    // Giới hạn số lần sử dụng cho user này (toàn bộ thời gian, null = không giới hạn)
    @Column(nullable = true)
    private Integer usageLimit;

    // Số lần đã dùng (toàn bộ thời gian)
    @Column(nullable = false)
    private Integer usedCount = 0;

    // Giới hạn số lần sử dụng mỗi ngày (null = không giới hạn)
    @Column(nullable = true)
    private Integer dailyLimit;

    // Số lần đã dùng hôm nay
    @Column(nullable = false)
    private Integer usedToday = 0;

    // Ngày cập nhật số lần dùng hôm nay
    @Column(nullable = true)
    private LocalDate lastUsedDate;

    // Ngày bắt đầu áp dụng cho khách hàng
    @Column(nullable = true)
    private LocalDateTime startAt;

    // Ngày kết thúc áp dụng cho khách hàng
    @Column(nullable = true)
    private LocalDateTime endAt;

    // Giới hạn theo số tháng kể từ ngày tạo user
    @Column(nullable = true)
    private Integer validMonths; // ví dụ: 3 tháng cho khách mới

    // Giới hạn theo số năm kể từ ngày tạo user
    @Column(nullable = true)
    private Integer validYears; // ví dụ: 1 năm cho khách trung thành

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Reset số lần dùng hôm nay nếu qua ngày mới
     */
    public void resetDailyUsageIfNeeded() {
        LocalDate today = LocalDate.now();
        if (lastUsedDate == null || !lastUsedDate.equals(today)) {
            usedToday = 0;
            lastUsedDate = today;
        }
    }

    /**
     * Kiểm tra còn hạn mức dùng hôm nay
     */
    public boolean canUseToday() {
        resetDailyUsageIfNeeded();
        if (dailyLimit == null) return true;
        return usedToday < dailyLimit;
    }

    /**
     * Kiểm tra còn hạn mức dùng tổng cộng
     */
    public boolean canUseOverall() {
        if (usageLimit == null) return true;
        return usedCount < usageLimit;
    }

    /**
     * Kiểm tra xem user còn trong thời gian áp dụng promotion
     */
    public boolean isWithinValidPeriod(LocalDateTime userCreatedAt) {
        LocalDateTime now = LocalDateTime.now();

        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;

        if (validMonths != null && userCreatedAt.plusMonths(validMonths).isBefore(now)) return false;
        if (validYears != null && userCreatedAt.plusYears(validYears).isBefore(now)) return false;

        return true;
    }

    /**
     * Ghi nhận đã dùng 1 lần
     */
    public void use() {
        resetDailyUsageIfNeeded();
        usedCount++;
        usedToday++;
        lastUsedDate = LocalDate.now();
    }
}