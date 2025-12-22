package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerIncidentStatsDTO;
import com.logistics.entity.IncidentReport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentReportRepository
        extends JpaRepository<IncidentReport, Integer>, JpaSpecificationExecutor<IncidentReport> {
            // Thống kê theo officeId 
    @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerIncidentStatsDTO(" +
            "COUNT(i), " +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.PENDING AND i.priority = com.logistics.enums.IncidentPriority.LOW THEN 1 ELSE 0 END), "
            +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.PENDING AND i.priority = com.logistics.enums.IncidentPriority.MEDIUM THEN 1 ELSE 0 END), "
            +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.PENDING AND i.priority = com.logistics.enums.IncidentPriority.HIGH THEN 1 ELSE 0 END), "
            +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.PENDING THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.PROCESSING THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.RESOLVED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN i.status = com.logistics.enums.IncidentStatus.REJECTED THEN 1 ELSE 0 END)) " +
            "FROM IncidentReport i WHERE i.office.id = :officeId")
    ManagerIncidentStatsDTO getIncidentStatsByOffice(@Param("officeId") Integer officeId);
}