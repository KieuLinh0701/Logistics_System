package com.logistics.repository;

import com.logistics.entity.EmployeeLeaveRequest;
import com.logistics.enums.EmployeeShift;
import com.logistics.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface EmployeeLeaveRequestRepository extends JpaRepository<EmployeeLeaveRequest, Integer> {
    List<EmployeeLeaveRequest> findByEmployeeId(Integer employeeId);

    List<EmployeeLeaveRequest> findByOfficeId(Integer officeId);

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
            FROM EmployeeLeaveRequest l
            WHERE l.employee.id = :employeeId
              AND l.leaveDate = :leaveDate
              AND l.status = :status
            """)
    boolean existsApprovedLeaveOnDate(
            @Param("employeeId") Integer employeeId,
            @Param("leaveDate") LocalDate leaveDate,
            @Param("status") LeaveRequestStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
            FROM EmployeeLeaveRequest l
            WHERE l.employee.id = :employeeId
              AND l.leaveDate = :leaveDate
              AND l.shift = :shift
              AND l.status IN :statuses
            """)
    boolean existsByEmployeeIdAndLeaveDateAndShiftAndStatusIn(
            @Param("employeeId") Integer employeeId,
            @Param("leaveDate") LocalDate leaveDate,
            @Param("shift") EmployeeShift shift,
            @Param("statuses") Set<LeaveRequestStatus> statuses);
}