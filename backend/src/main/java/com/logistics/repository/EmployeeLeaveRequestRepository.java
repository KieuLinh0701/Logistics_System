package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.entity.EmployeeLeaveRequest;

@Repository
public interface EmployeeLeaveRequestRepository extends JpaRepository<EmployeeLeaveRequest, Integer> {
    List<EmployeeLeaveRequest> findByEmployeeId(Integer employeeId);

    List<EmployeeLeaveRequest> findByOfficeId(Integer officeId);
}