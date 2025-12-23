package com.logistics.dto.manager.shipperAssignment;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipperAssignmentDto {
    private Long id;
    private Integer wardCode;
    private Integer cityCode;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}