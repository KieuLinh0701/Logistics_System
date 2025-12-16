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
public class ManagerShipperAssignmentListDto {
    private Long id;
    private Integer wardCode;
    private Integer cityCode;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Employee employee;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Employee {
        private Integer id;
        private String code;
        private String lastName;
        private String firstName;
        private String phoneNumber;
        private String email;
        private String shift;
        private String status;
    }
}