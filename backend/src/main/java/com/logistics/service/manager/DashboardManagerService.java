package com.logistics.service.manager;

import com.logistics.dto.manager.dashboard.ManagerDashboardOverviewResponseDTO;

public interface DashboardManagerService {
    ManagerDashboardOverviewResponseDTO getOverview(Integer userId);
}