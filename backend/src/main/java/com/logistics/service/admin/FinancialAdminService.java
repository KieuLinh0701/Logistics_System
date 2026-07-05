package com.logistics.service.admin;

import com.logistics.dto.admin.AdminPaymentSubmissionListDto;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ListResponse;

import java.util.Map;

public interface FinancialAdminService {
    ListResponse<AdminPaymentSubmissionListDto> listSubmissions(String status);
    ListResponse<PaymentSubmissionBatch> listBatches();
    ListResponse<PaymentSubmissionBatch> listBatches(int page, int limit, String search, String status, Integer shipperId);
    Map<Integer, java.util.List<AdminPaymentSubmissionListDto>> listPendingGroupedByShipper();
    void processSubmission(Integer adminId, Integer submissionId, CreatePaymentSubmissionRequest form);
    PaymentSubmissionBatch getBatchById(Integer id);
    byte[] exportBatches(int page, int limit, String search, String status, Integer shipperId);
    byte[] exportSubmissions(String status, String search);
    void completeBatch(Integer adminId, Integer batchId);
}
