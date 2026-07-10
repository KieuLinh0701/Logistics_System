package com.logistics.service.manager;

import com.logistics.dto.manager.attempt.AttemptHistoryListDto;
import com.logistics.request.manager.attempt.AttemptSearchRequest;
import com.logistics.response.ListResponse;

public interface AttemptHistoryManagerService {
    ListResponse<AttemptHistoryListDto> list(int userId, AttemptSearchRequest request);
}
