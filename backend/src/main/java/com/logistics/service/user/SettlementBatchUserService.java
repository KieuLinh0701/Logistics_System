package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.user.UserSettlementBatchListDto;
import com.logistics.dto.user.UserSettlementOrderDto;
import com.logistics.dto.user.UserSettlementTransactionDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.entity.Order;
import com.logistics.entity.SettlementBatch;
import com.logistics.entity.SettlementTransaction;
import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.SettlementBatchMapper;
import com.logistics.mapper.SettlementTransactionMapper;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.SettlementBatchRepository;
import com.logistics.repository.SettlementTransactionRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.OrderSpecification;
import com.logistics.specification.SettlementBatchSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementBatchUserService {

    private final SettlementBatchRepository batchRepository;

    private final SettlementTransactionRepository transactionRepository;

    private final OrderRepository orderRepository;

    public ApiResponse<ListResponse<UserSettlementBatchListDto>> list(
            Integer userId, SearchRequest request) {
        try {
            Sort sort = buildSort(request.getSort());
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
            List<SettlementBatch> batchs = getSettlementBatchs(userId, request, pageable);

            List<UserSettlementBatchListDto> list = batchs.stream()
                    .map(batch -> {
                        BigDecimal remainAmount = calculateRemainAmount(batch);
                        return SettlementBatchMapper.toListDtos(batch, remainAmount);
                    })
                    .toList();

            int total = batchs.size();
            Pagination pagination = new Pagination(total, request.getPage(), request.getLimit(), 1);

            ListResponse<UserSettlementBatchListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách phiên đối soát thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private List<SettlementBatch> getSettlementBatchs(Integer userId, SearchRequest request,
            Pageable pageable) {

        System.out.println("type" + request.getType());

        Specification<SettlementBatch> spec = SettlementBatchSpecification.unrestricted()
                .and(SettlementBatchSpecification.userId(userId))
                .and(SettlementBatchSpecification.search(request.getSearch()))
                .and(SettlementBatchSpecification.status(request.getStatus()))
                .and(SettlementBatchSpecification.balanceType(request.getType()))
                .and(SettlementBatchSpecification.createdAtBetween(
                        parseDate(request.getStartDate()), parseDate(request.getEndDate())));

        if (pageable != null) {
            return batchRepository.findAll(spec, pageable).getContent();
        } else {
            return batchRepository.findAll(spec, buildSort(request.getSort()));
        }
    }

    private BigDecimal calculateRemainAmount(SettlementBatch batch) {

        BigDecimal balance = batch.getBalanceAmount();
        if (balance == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal paidAmount = transactionRepository.sumAmountByBatchAndStatus(
                batch.getId(),
                SettlementTransactionStatus.SUCCESS);

        return balance.abs().subtract(paidAmount);
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
}