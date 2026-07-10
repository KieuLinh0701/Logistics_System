package com.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAttemptDto {
    private String attemptType;
    private Integer attemptNumber;
    private String status;
    private String failReason;
    private String note;
    private String proofImageUrl;
    private LocalDateTime attemptedAt;
    private String shipperName;
}
