package com.logistics.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PickupAttemptDto {
    private Integer attemptNumber;
    private String status;
    private String failReason;
    private String note;
    private LocalDateTime attemptedAt;
    private String shipperName;
}
