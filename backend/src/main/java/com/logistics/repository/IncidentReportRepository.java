package com.logistics.repository;

import com.logistics.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface IncidentReportRepository
                extends JpaRepository<IncidentReport, Integer>, JpaSpecificationExecutor<IncidentReport> {
}
