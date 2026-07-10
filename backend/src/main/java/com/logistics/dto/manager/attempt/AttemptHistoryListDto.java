package com.logistics.dto.manager.attempt;

import com.logistics.enums.DeliveryAttemptStatus;
import com.logistics.enums.DeliveryAttemptType;
import com.logistics.enums.PickupAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptHistoryListDto {

    private Long id;

    private String attemptCategory;

    private DeliveryAttemptType attemptType;

    private Integer orderId;
    private String trackingNumber;

    private Integer attemptNumber;

    private PickupAttemptStatus pickupStatus;
    private DeliveryAttemptStatus deliveryStatus;

    private String failReason;

    private String note;
    private String proofImageUrl;

    private LocalDateTime attemptedAt;

    private Integer shipperId;
    private String shipperName;
    private String shipperPhone;
}
