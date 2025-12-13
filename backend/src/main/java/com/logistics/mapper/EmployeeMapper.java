package com.logistics.mapper;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.entity.Address;
import com.logistics.entity.Employee;

public class EmployeeMapper {

    public static ManagerEmployeeListDto toManagerEmployeeListDto(Employee entity, Address address) {
        if (entity == null) {
            return null;
        }

        return new ManagerEmployeeListDto(
                entity.getId(),
                entity.getCode(),
                entity.getUser() != null ? entity.getUser().getLastName() : null,
                entity.getUser() != null ? entity.getUser().getFirstName() : null,
                entity.getUser() != null ? entity.getUser().getPhoneNumber(): null,
                address != null ? address.getCityCode() : null,
                address != null ? address.getWardCode() : null,
                address != null ? address.getDetail() : null,
                entity.getAccountRole() != null ? entity.getAccountRole().getRole().getName() : null,
                entity.getUser() != null ? entity.getUser().getAccount().getEmail() : null,
                entity.getHireDate(),
                entity.getShift().name(),
                entity.getStatus().name());
    }
}