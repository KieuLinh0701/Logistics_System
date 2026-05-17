package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.logistics.dto.user.settlement.UserSettlementSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.user.settlement.UserSettlementBatchListDto;
import com.logistics.dto.user.settlement.UserSettlementOrderDto;
import com.logistics.dto.user.settlement.UserSettlementTransactionDto;
import com.logistics.dto.user.dashboard.UserRevenueStatsDTO;
import com.logistics.entity.Order;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.SettlementStatus;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.SettlementBatchMapper;
import com.logistics.mapper.SettlementTransactionMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.OrderSpecification;
import com.logistics.specification.SettlementBatchSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementBatchUserService {

    private final SettlementBatchRepository batchRepository;

    private final SettlementTransactionRepository transactionRepository;

    private final OrderRepository orderRepository;

    private final UserSettlementScheduleUserService scheduleUserService;

    public ApiResponse<UserSettlementSummaryResponse> getSummary(Integer userId) {
        try {
            List<SettlementBatch> batches = batchRepository.findByShop_Id(userId);

            BigDecimal received = batches.stream()
                    .filter(b -> b.getStatus() == SettlementStatus.COMPLETED
                            && b.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(SettlementBatch::getBalanceAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pending = batches.stream()
                    .filter(b -> b.getStatus() != SettlementStatus.COMPLETED
                            && b.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(SettlementBatch::getBalanceAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal debt = batches.stream()
                    .filter(b -> b.getStatus() != SettlementStatus.COMPLETED
                            && b.getBalanceAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(b -> b.getBalanceAmount().abs().subtract(b.getPaidAmount()))
                    .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new ApiResponse<>(true, "Lấy tổng quan đối soát thành công",
                    new UserSettlementSummaryResponse(received, pending, debt));

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ListResponse<UserSettlementBatchListDto>> list(
            Integer userId, SearchRequest request) {
        try {
            Sort sort = buildSort(request.getSort());
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
            Page<SettlementBatch> pageData = getSettlementBatchs(userId, request, pageable);

            List<UserSettlementBatchListDto> list = pageData.getContent()
                    .stream()
                    .map(SettlementBatchMapper::toListDtos)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) pageData.getTotalElements(),
                    request.getPage(),
                    request.getLimit(),
                    pageData.getTotalPages());

            ListResponse<UserSettlementBatchListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách phiên đối soát thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private Page<SettlementBatch> getSettlementBatchs(
            Integer userId,
            SearchRequest request,
            Pageable pageable) {

        Specification<SettlementBatch> spec = SettlementBatchSpecification.unrestricted()
                .and(SettlementBatchSpecification.userId(userId))
                .and(SettlementBatchSpecification.search(request.getSearch()))
                .and(SettlementBatchSpecification.status(request.getStatus()))
                .and(SettlementBatchSpecification.balanceType(request.getType()))
                .and(SettlementBatchSpecification.createdAtBetween(
                        parseDate(request.getStartDate()),
                        parseDate(request.getEndDate())));

        return batchRepository.findAll(spec, pageable);
    }

    private Sort buildSort(String sort) {
        if (sort == null)
            return Sort.unsorted();
        return switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "balance_high" -> Sort.by("balanceAmount").descending();
            case "balance_low" -> Sort.by("balanceAmount").ascending();
            default -> Sort.unsorted();
        };
    }

    private LocalDateTime parseDate(String s) {
        return (s != null && !s.isBlank()) ? LocalDateTime.parse(s) : null;
    }

    public ApiResponse<ListResponse<UserSettlementOrderDto>> getOrdersBySettlementBatchId(int userId, Integer batchId,
            SearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String status = request.getStatus();
            String sort = request.getSort();
            String cod = request.getCod();
            String payer = request.getPayer();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                    .and(OrderSpecification.settlementBatchId(batchId))
                    .and(OrderSpecification.userId(userId))
                    .and(OrderSpecification.settlementSearch(search))
                    .and(OrderSpecification.payer(payer))
                    .and(OrderSpecification.status(status))
                    .and(OrderSpecification.cod(cod))
                    .and(OrderSpecification.createdAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("createdAt").descending();
                case "oldest" -> Sort.by("createdAt").ascending();
                case "cod_high" -> Sort.by("cod").descending();
                case "cod_low" -> Sort.by("cod").ascending();
                case "fee_high" -> Sort.by("totalFee").descending();
                case "fee_low" -> Sort.by("totalFee").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Order> pageData = orderRepository.findAll(spec, pageable);

            List<UserSettlementOrderDto> list = pageData.getContent()
                    .stream()
                    .map(order -> OrderMapper.toUserSettlementOrderDto(order))
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<UserSettlementOrderDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<UserSettlementTransactionDto>> getSettlementTransactionsBySettlementBatchId(
            Integer userId, Integer batchId) {
        try {
            Sort sort = Sort.by(Sort.Direction.DESC, "paidAt");

            SettlementBatch batch = batchRepository
                    .findByIdAndShop_Id(batchId, userId)
                    .orElseThrow(() -> new RuntimeException("Không có quyền truy cập"));

            List<SettlementTransaction> transactions = transactionRepository.findBySettlementBatchId(batchId, sort);

            List<UserSettlementTransactionDto> list = transactions.stream()
                    .map(tx -> SettlementTransactionMapper.toUserSettlementTransactionDto(tx))
                    .toList();

            return new ApiResponse<>(
                    true,
                    "Lấy danh sách giao dịch đối soát thành công",
                    list);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserSettlementBatchListDto> getBySettlementBatchId(Integer userId, Integer batchId) {
        try {
            SettlementBatch batch = batchRepository.findByIdAndShop_Id(batchId, userId)
                    .orElse(null);

            if (batch == null) {
                return new ApiResponse<>(false, "Không tìm thấy phiên đối soát của bạn", null);
            }
            UserSettlementBatchListDto dto = SettlementBatchMapper.toListDtos(batch);

            return new ApiResponse<>(true, "Lấy thông tin phiên đối soát thành công", dto);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public UserRevenueStatsDTO getUserRevenueStats(Integer userId) {

        BigDecimal received = batchRepository.sumReceivedByUser(userId);

        BigDecimal nextSettlement = orderRepository.sumPendingCODNow(
                userId, List.of(OrderStatus.DELIVERED, OrderStatus.RETURNED));

        BigDecimal pendingDebt = calculatePendingDebt(userId);

        String nextSettlementDate = scheduleUserService.getNextSettlementDate(userId);

        return new UserRevenueStatsDTO(
                received,
                nextSettlement,
                pendingDebt,
                nextSettlementDate);
    }

    public BigDecimal calculatePendingDebt(Integer userId) {
        List<SettlementBatch> batches = batchRepository.findDebtBatchesByUser(userId);

        BigDecimal totalDebt = BigDecimal.ZERO;

        for (SettlementBatch b : batches) {
            BigDecimal originalDebt = b.getBalanceAmount().abs();

            if (b.getStatus() == SettlementStatus.PENDING
                    || b.getStatus() == SettlementStatus.FAILED) {

                totalDebt = totalDebt.add(originalDebt);
            }
        }

        return totalDebt;
    }

}