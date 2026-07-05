package com.logistics.service.shipper;

import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;

import java.util.Map;

public interface CODShipperService {

    Map<String, Object> getCODTransactions(int page, int limit, String status, String dateFrom, String dateTo);

    Map<String, Object> collectCOD(CollectCODRequest request);

    Map<String, Object> submitCOD(SubmitCODRequest request);

    Map<String, Object> getCODSubmissionHistory(int page, int limit, String status, String dateFrom, String dateTo);
}
