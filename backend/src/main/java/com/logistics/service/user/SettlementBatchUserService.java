package com.logistics.service.user;

import com.logistics.dto.user.dashboard.UserRevenueStatsDTO;
import com.logistics.dto.user.settlement.UserSettlementBatchListDto;
import com.logistics.dto.user.settlement.UserSettlementOrderDto;
import com.logistics.dto.user.settlement.UserSettlementSummaryResponse;
import com.logistics.dto.user.settlement.UserSettlementTransactionDto;
import com.logistics.request.SearchRequest;
import com.logistics.response.ListResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SettlementBatchUserService {
    UserSettlementSummaryResponse getSummary(Integer userId);
    ListResponse<UserSettlementBatchListDto> list(Integer userId, SearchRequest request);
    ListResponse<UserSettlementOrderDto> getOrdersBySettlementBatchId(int userId, Integer batchId, SearchRequest request);
    List<UserSettlementTransactionDto> getSettlementTransactionsBySettlementBatchId(Integer userId, Integer batchId);
    UserSettlementBatchListDto getBySettlementBatchId(Integer userId, Integer batchId);
    UserRevenueStatsDTO getUserRevenueStats(Integer userId);
    BigDecimal calculatePendingDebt(Integer userId);
    byte[] export(Integer userId, SearchRequest request);
    byte[] exportById(Integer userId, Integer settlementBatchId);
}