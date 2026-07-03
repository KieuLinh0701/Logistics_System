package com.logistics.service.manager;

import com.logistics.dto.OfficeDto;
import com.logistics.request.manager.ManagerOfficeEditRequest;

public interface OfficeManagerService {
    OfficeDto getMyOffice(int userId);
    void updateMyOffice(int userId, ManagerOfficeEditRequest request);
    Integer getMyOfficeCityCode(int userId);
}