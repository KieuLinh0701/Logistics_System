package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerEmployeeStatsDTO;
import com.logistics.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByCode(String code);

    List<Employee> findByOfficeId(Integer officeId);

    List<Employee> findByUserId(Integer userId);

    boolean existsByCode(String code);

    List<Employee> findAllByAccountRoleId(Integer accountRoleId);

    @Query("select e from Employee e where e.accountRole.account.id = :accountId")
    List<Employee> findAllByAccountId(@Param("accountId") Integer accountId);

    // nếu hữu ích: tìm employee theo accountRole (single optional)
    Optional<Employee> findByAccountRoleId(Integer accountRoleId);

    // Thống kê theo officeId
    @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerEmployeeStatsDTO(" +
            "COUNT(e), " +
            "SUM(CASE WHEN e.status = com.logistics.enums.EmployeeStatus.ACTIVE THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN e.status = com.logistics.enums.EmployeeStatus.INACTIVE THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN e.status = com.logistics.enums.EmployeeStatus.LEAVE THEN 1 ELSE 0 END)) " +
            "FROM Employee e WHERE e.office.id = :officeId")
    ManagerEmployeeStatsDTO getEmployeeStatsByOffice(@Param("officeId") Integer officeId);

    @Query("SELECT e.shift, COUNT(e) " +
            "FROM Employee e " +
            "WHERE e.office.id = :officeId AND e.status = com.logistics.enums.EmployeeStatus.ACTIVE " +
            "GROUP BY e.shift")
    List<Object[]> countActiveEmployeesByShiftForOffice(@Param("officeId") Integer officeId);

}