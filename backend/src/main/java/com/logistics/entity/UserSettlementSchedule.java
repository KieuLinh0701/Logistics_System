// Bảng sắp xếp lịch đối soát của người dùng chọn
package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

import com.logistics.enums.UserSettlementSchedule.WeekDay;

@Entity
@Table(name = "user_settlement_schedules")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettlementSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người dùng chọn lịch
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ngày trong tuần user muốn đối soát (có thể nhiều ngày)
    @ElementCollection(targetClass = WeekDay.class)
    @CollectionTable(name = "user_settlement_weekdays", 
                     joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "weekday")
    private Set<WeekDay> weekdays;
}