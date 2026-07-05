package com.logistics.service.admin;

import com.logistics.request.admin.CreateFeeConfigurationRequest;
import com.logistics.request.admin.UpdateFeeConfigurationRequest;

import java.util.Map;

public interface FeeConfigurationAdminService {

    Map<String, Object> listFeeConfigurations(int page, int limit, String search, String feeType, Integer serviceTypeId, Boolean active);

    Map<String, Object> getFeeConfigurationById(Integer id);

    void createFeeConfiguration(CreateFeeConfigurationRequest request);

    void updateFeeConfiguration(Integer id, UpdateFeeConfigurationRequest request);

    void deleteFeeConfiguration(Integer id);
}
