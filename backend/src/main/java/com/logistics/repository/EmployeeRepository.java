package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerEmployeeStatsDTO;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.entity.Employee;
import com.logistics.enums.EmployeeShift;
import com.logistics.enums.EmployeeStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
                SELECT new com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto(
                    e.id,
                    CONCAT(u.lastName, ' ', u.firstName),
                    e.code,
                    r.name,
                    u.phoneNumber,
                    CAST(e.status AS string),
                    CAST(e.shift AS string),

                    COUNT(DISTINCT s.id),
                    COUNT(so.id),

                    COALESCE(
                        SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END),
                        0
                    ),

                    CASE
                        WHEN COUNT(so.id) = 0 THEN 0
                        ELSE
                            SUM(
                                FUNCTION('TIMESTAMPDIFF', MINUTE, s.startTime, s.endTime)
                            ) * 1.0 / COUNT(so.id)
                    END
                )
                FROM Employee e
                    JOIN e.user u
                    JOIN e.office office
                    JOIN e.accountRole ar
                    JOIN ar.role r

                    LEFT JOIN Shipment s ON s.employee.id = e.id
                    LEFT JOIN ShipmentOrder so ON so.shipment.id = s.id
                    LEFT JOIN so.order o

                WHERE office.id = :officeId
                  AND (
                        :search IS NULL
                        OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(CONCAT(u.lastName, ' ', u.firstName))
                            LIKE LOWER(CONCAT('%', :search, '%'))
                        OR u.phoneNumber LIKE CONCAT('%', :search, '%')
                  )
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:shift IS NULL OR e.shift = :shift)
                  AND (:status IS NULL OR e.status = :status)
                  AND r.isSystemRole = true
                  AND r.name <> 'Manager'

                GROUP BY
                    e.id,
                    u.lastName,
                    u.firstName,
                    e.code,
                    r.name,
                    u.phoneNumber,
                    e.status,
                    e.shift
            """,

            countQuery = """
                        SELECT COUNT(DISTINCT e.id)
                        FROM Employee e
                            JOIN e.user u
                            JOIN e.office office
                            JOIN e.accountRole ar
                            JOIN ar.role r

                        WHERE office.id = :officeId
                          AND (
                                :search IS NULL
                                OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(CONCAT(u.lastName, ' ', u.firstName))
                                    LIKE LOWER(CONCAT('%', :search, '%'))
                                OR u.phoneNumber LIKE CONCAT('%', :search, '%')
                          )
                          AND (:roleName IS NULL OR r.name = :roleName)
                          AND (:shift IS NULL OR e.shift = :shift)
                          AND (:status IS NULL OR e.status = :status)
                          AND r.isSystemRole = true
                          AND r.name <> 'Manager'
                    """)
    Page<ManagerEmployeePerformanceDto> getEmployeePerformance(
            @Param("officeId") Integer officeId,
            @Param("search") String search,
            @Param("roleName") String roleName,
            @Param("shift") EmployeeShift shift,
            @Param("status") EmployeeStatus status,
            Pageable pageable);

    @Query("""
                SELECT new com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto(
                    e.id,
                    CONCAT(u.lastName, ' ', u.firstName),
                    e.code,
                    r.name,
                    u.phoneNumber,
                    CAST(e.status AS string),
                    CAST(e.shift AS string),

                    COUNT(DISTINCT s.id),
                    COUNT(so.id),

                    COALESCE(
                        SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END),
                        0
                    ),

                    CASE
                        WHEN COUNT(so.id) = 0 THEN 0
                        ELSE
                            SUM(
                                FUNCTION('TIMESTAMPDIFF', MINUTE, s.startTime, s.endTime)
                            ) * 1.0 / COUNT(so.id)
                    END
                )
                FROM Employee e
                    JOIN e.user u
                    JOIN e.office office
                    JOIN e.accountRole ar
                    JOIN ar.role r

                    LEFT JOIN Shipment s ON s.employee.id = e.id
                    LEFT JOIN ShipmentOrder so ON so.shipment.id = s.id
                    LEFT JOIN so.order o

                WHERE office.id = :officeId
                  AND (
                        :search IS NULL
                        OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(CONCAT(u.lastName, ' ', u.firstName))
                            LIKE LOWER(CONCAT('%', :search, '%'))
                        OR u.phoneNumber LIKE CONCAT('%', :search, '%')
                  )
                  AND (:roleName IS NULL OR r.name = :roleName)
                  AND (:shift IS NULL OR e.shift = :shift)
                  AND (:status IS NULL OR e.status = :status)
                  AND r.isSystemRole = true
                  AND r.name <> 'Manager'

                GROUP BY
                    e.id,
                    u.lastName,
                    u.firstName,
                    e.code,
                    r.name,
                    u.phoneNumber,
                    e.status,
                    e.shift
            """)
    List<ManagerEmployeePerformanceDto> getEmployeePerformanceList(
            @Param("officeId") Integer officeId,
            @Param("search") String search,
            @Param("roleName") String roleName,
            @Param("shift") EmployeeShift shift,
            @Param("status") EmployeeStatus status);
}