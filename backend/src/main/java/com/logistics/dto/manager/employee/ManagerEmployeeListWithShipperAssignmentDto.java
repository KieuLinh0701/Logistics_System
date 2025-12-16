package com.logistics.dto.manager.employee;

import java.util.List;

import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeeListWithShipperAssignmentDto {

    private Employee employee;
    private List<ManagerShipperAssignmentDto> assignments;

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
