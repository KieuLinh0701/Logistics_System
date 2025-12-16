package com.logistics.mapper;

import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentListDto;
import com.logistics.entity.Employee;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public class ShipperAssignmentMapper {

        public static ManagerShipperAssignmentListDto toListDto(ShipperAssignment assignment) {
                if (assignment == null)
                        return null;

                User shipperUser = assignment.getShipper();

                Employee shipperEmployee = shipperUser.getEmployees().stream()
                                .filter(e -> e.getAccountRole() != null
                                                && e.getAccountRole().getRole() != null
                                                && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                                .findFirst()
                                .orElse(null);

                ManagerShipperAssignmentListDto.Employee employeeDto = null;
                if (shipperEmployee != null) {
                        employeeDto = new ManagerShipperAssignmentListDto.Employee(
                                        shipperEmployee.getId(),
                                        shipperEmployee.getCode(),
                                        shipperUser.getLastName(),
                                        shipperUser.getFirstName(),
                                        shipperUser.getPhoneNumber(),
                                        shipperUser.getAccount() != null ? shipperUser.getAccount().getEmail() : null,
                                        shipperEmployee.getShift() != null ? shipperEmployee.getShift().name() : null,
                                        shipperEmployee.getStatus() != null ? shipperEmployee.getStatus().name()
                                                        : null);
                }

                return new ManagerShipperAssignmentListDto(
                                assignment.getId(),
                                assignment.getWardCode(),
                                assignment.getCityCode(),
                                assignment.getStartAt(),
                                assignment.getEndAt(),
                                assignment.getNotes(),
                                assignment.getCreatedAt(),
                                assignment.getUpdatedAt(),
                                employeeDto);
        }

        public static List<ManagerShipperAssignmentListDto> toListDto(List<ShipperAssignment> assignments) {
                if (assignments == null)
                        return null;

                return assignments.stream()
                                .map(ShipperAssignmentMapper::toListDto)
                                .collect(Collectors.toList());
        }
}