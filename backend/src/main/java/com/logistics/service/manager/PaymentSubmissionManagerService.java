package com.logistics.service.manager;

import com.logistics.dto.manager.paymentSubmission.ManagerPaymentSubmissionListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.ManagerPaymentSubmissionForm;
import com.logistics.response.ListResponse;

public interface PaymentSubmissionManagerService {
    ListResponse<ManagerPaymentSubmissionListDto> list(Integer userId, Integer batchId, SearchRequest request);
    byte[] export(Integer userId, Integer batchId, SearchRequest request);
    void processing(int userId, int id, ManagerPaymentSubmissionForm request);
}