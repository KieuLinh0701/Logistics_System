package com.logistics.service.manager;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchEditForm;
import com.logistics.response.ListResponse;

public interface PaymentSubmissionBatchManagerService {
    ListResponse<ManagerPaymentSubmissionBatchListDto> list(Integer userId, SearchRequest request);
    byte[] export(Integer userId, SearchRequest request);
    void processing(Integer userId, Integer id, ManagerPaymentSubmissionBatchEditForm request);
}