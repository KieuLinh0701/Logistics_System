package com.logistics.mapper;

import java.util.List;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentDto;
import com.logistics.entity.Employee;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.User;

public class EmployeeMapper {

        public static ManagerEmployeeListWithShipperAssignmentDto toManagerEmployeeListDto(
                        User user,
                        String employeeCode,
                        Integer employeeId,
                        List<ShipperAssignment> activeAssignments) {

                if (user == null) {
                        return null;
                }

                ManagerEmployeeListWithShipperAssignmentDto.Employee empDto = new ManagerEmployeeListWithShipperAssignmentDto.Employee(
                                employeeId,
                                employeeCode,
                                user.getLastName(),
                                user.getFirstName(),
                                user.getPhoneNumber(),
                                user.getAccount() != null ? user.getAccount().getEmail() : null,
                                user.getEmployees().stream()
                                                .filter(emp -> emp.getAccountRole() != null
                                                                && "Shipper".equals(emp.getAccountRole().getRole()
                                                                                .getName()))
                                                .findFirst()
                                                .map(emp -> emp.getShift().name())
                                                .orElse(null),
                                user.getEmployees().stream()
                                                .filter(emp -> emp.getAccountRole() != null
                                                                && "Shipper".equals(emp.getAccountRole().getRole()
                                                                                .getName()))
                                                .findFirst()
                                                .map(emp -> emp.getStatus().name())
                                                .orElse(null));

                List<ManagerShipperAssignmentDto> assignmentDtos = activeAssignments.stream()
                                .map(sa -> new ManagerShipperAssignmentDto(
                                                sa.getId(),
                                                sa.getWardCode(),
                                                sa.getCityCode(),
                                                sa.getStartAt(),
                                                sa.getEndAt(),
                                                sa.getNotes(),
                                                sa.getCreatedAt(),
                                                sa.getUpdatedAt()))
                                .toList();

                ManagerEmployeeListWithShipperAssignmentDto dto = new ManagerEmployeeListWithShipperAssignmentDto();
                dto.setEmployee(empDto);
                dto.setAssignments(assignmentDtos);

                return dto;
        }

        public static ManagerEmployeeListDto toManagerEmployeeListDto(Employee entity) {
                if (entity == null) {
                        return null;
                }

                return new ManagerEmployeeListDto(
                                entity.getId(),
                                entity.getCode(),
                                entity.getUser() != null ? entity.getUser().getLastName() : null,
                                entity.getUser() != null ? entity.getUser().getFirstName() : null,
                                entity.getUser() != null ? entity.getUser().getPhoneNumber() : null,
                                entity.getAccountRole() != null ? entity.getAccountRole().getRole().getName() : null,
                                entity.getUser() != null ? entity.getUser().getAccount().getEmail() : null,
                                entity.getHireDate(),
                                entity.getShift().name(),
                                entity.getStatus().name());
        }
}