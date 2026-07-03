package com.logistics.service.manager;

import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.incidentReport.ManagerIncidentUpdateRequest;
import com.logistics.response.ListResponse;

public interface IncidentReportManagerService {
    ListResponse<ManagerIncidentReportListDto> list(int userId, SearchRequest request);
    ManagerIncidentReportDetailDto getById(int userId, int id);
    void processing(int userId, int id, ManagerIncidentUpdateRequest request);
    byte[] export(int userId, SearchRequest request);
}