package com.logistics.service.admin;

import com.logistics.request.admin.CreateOfficeRequest;
import com.logistics.request.admin.UpdateOfficeRequest;

import java.util.Map;

public interface OfficeAdminService {

    Map<String, Object> listOffices(int page, int limit, String search);

    Map<String, Object> getOfficeById(Integer officeId);

    void createOffice(CreateOfficeRequest request);

    void updateOffice(Integer officeId, UpdateOfficeRequest request);

    void deleteOffice(Integer officeId);
}
