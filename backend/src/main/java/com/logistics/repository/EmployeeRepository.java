package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerEmployeeStatsDTO;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.entity.Employee;
import com.logistics.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {
    List<Employee> findByOfficeId(Integer officeId);

    List<Employee> findByUserId(Integer userId);

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
            SELECT
                e.id                                                     AS id,
                CONCAT(u.last_name, ' ', u.first_name)                  AS employeeName,
                e.code                                                   AS employeeCode,
                r.name                                                   AS employeeRole,
                u.phone_number                                           AS employeePhone,
                e.status                                                 AS employeeStatus,
                e.shift                                                  AS employeeShift,
                COUNT(DISTINCT s.id)                                     AS totalShipments,
                COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END) AS totalOrders,
                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), 0)                        AS completedOrders,
                CASE
                    WHEN COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END) = 0
                    THEN 0.0
                    ELSE SUM(TIMESTAMPDIFF(MINUTE, s.start_time, s.end_time)) * 1.0
                         / COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END)
                END                                                      AS avgTimePerOrder
            FROM employees e
                JOIN users u ON u.id = e.user_id
                JOIN offices off ON off.id = e.office_id
                JOIN account_roles ar ON ar.id = e.account_role_id
                JOIN roles r ON r.id = ar.role_id
                LEFT JOIN shipments s ON s.employee_id = e.id
                LEFT JOIN shipment_orders so ON so.shipment_id = s.id
                LEFT JOIN orders o ON o.id = so.order_id
            WHERE off.id = :officeId
              AND (:search IS NULL
                    OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(CONCAT(u.last_name, ' ', u.first_name)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR u.phone_number LIKE CONCAT('%', :search, '%'))
              AND r.name = 'Shipper'
              AND r.user_owner_id IS NULL
              AND (:shift IS NULL OR e.shift = :shift)
              AND (:status IS NULL OR e.status = :status)
            GROUP BY e.id, u.last_name, u.first_name, e.code, r.name, u.phone_number, e.status, e.shift
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT e.id)
                    FROM employees e
                        JOIN users u ON u.id = e.user_id
                        JOIN offices off ON off.id = e.office_id
                        JOIN account_roles ar ON ar.id = e.account_role_id
                        JOIN roles r ON r.id = ar.role_id
                    WHERE off.id = :officeId
                      AND (:search IS NULL
                            OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(CONCAT(u.last_name, ' ', u.first_name)) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR u.phone_number LIKE CONCAT('%', :search, '%'))
                      AND r.name = 'Shipper'
                      AND r.user_owner_id IS NULL
                      AND (:shift IS NULL OR e.shift = :shift)
                      AND (:status IS NULL OR e.status = :status)
                    """,
            nativeQuery = true)
    Page<ManagerEmployeePerformanceDto> getShipperPerformance(
            @Param("officeId") Integer officeId,
            @Param("search") String search,
            @Param("shift") String shift,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = """
            SELECT
                e.id                                                     AS id,
                CONCAT(u.last_name, ' ', u.first_name)                  AS employeeName,
                e.code                                                   AS employeeCode,
                r.name                                                   AS employeeRole,
                u.phone_number                                           AS employeePhone,
                e.status                                                 AS employeeStatus,
                e.shift                                                  AS employeeShift,
                COUNT(DISTINCT s.id)                                     AS totalShipments,
                COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END) AS totalOrders,
                COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), 0)                        AS completedOrders,
                CASE
                    WHEN COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END) = 0
                    THEN 0.0
                    ELSE SUM(TIMESTAMPDIFF(MINUTE, s.start_time, s.end_time)) * 1.0
                         / COUNT(CASE WHEN so.order_id IS NOT NULL AND so.shipment_id IS NOT NULL THEN 1 ELSE NULL END)
                END                                                      AS avgTimePerOrder
            FROM employees e
                JOIN users u ON u.id = e.user_id
                JOIN offices off ON off.id = e.office_id
                JOIN account_roles ar ON ar.id = e.account_role_id
                JOIN roles r ON r.id = ar.role_id
                LEFT JOIN shipments s ON s.employee_id = e.id
                LEFT JOIN shipment_orders so ON so.shipment_id = s.id
                LEFT JOIN orders o ON o.id = so.order_id
            WHERE off.id = :officeId
              AND (:search IS NULL
                    OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(CONCAT(u.last_name, ' ', u.first_name)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR u.phone_number LIKE CONCAT('%', :search, '%'))
              AND r.name = 'Shipper'
              AND r.user_owner_id IS NULL
              AND (:shift IS NULL OR e.shift = :shift)
              AND (:status IS NULL OR e.status = :status)
            GROUP BY e.id, u.last_name, u.first_name, e.code, r.name, u.phone_number, e.status, e.shift
            """, nativeQuery = true)
    List<ManagerEmployeePerformanceDto> getShipperPerformanceList(
            @Param("officeId") Integer officeId,
            @Param("search") String search,
            @Param("shift") String shift,
            @Param("status") String status);

    @EntityGraph(attributePaths = {"office"})
    Optional<Employee> findByUserIdAndStatus(Integer userId, EmployeeStatus status);

    @Query("""
            SELECT e FROM Employee e
            JOIN e.accountRole ar
            JOIN ar.role r
            JOIN ar.account a
            WHERE e.office.id = :officeId
            AND e.status = com.logistics.enums.EmployeeStatus.ACTIVE
            AND ar.isActive = true
            AND LOWER(r.name) = LOWER('Manager')
            """)
    List<Employee> findActiveManagersByOfficeId(@Param("officeId") Integer officeId);
}