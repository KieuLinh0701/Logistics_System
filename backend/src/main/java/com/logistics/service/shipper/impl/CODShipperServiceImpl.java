package com.logistics.service.shipper.impl;

import com.logistics.entity.*;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
import com.logistics.repository.*;
import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;
import com.logistics.response.Pagination;
import com.logistics.service.shipper.CODShipperService;
import com.logistics.utils.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CODShipperServiceImpl implements CODShipperService {

    private final PaymentSubmissionRepository paymentSubmissionRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final PaymentSubmissionBatchRepository paymentSubmissionBatchRepository;

    private User getCurrentShipperUser() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.getFirst().getUser();
    }

    private Employee getCurrentShipperEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.stream()
                .filter(e -> e.getAccountRole() != null
                        && e.getAccountRole().getRole() != null
                        && "Shipper".equals(e.getAccountRole().getRole().getName())
                        && e.getAccountRole().getRole().getUserOwner() == null
                        && EmployeeStatus.ACTIVE.equals(e.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    @Override
    public Map<String, Object> getCODTransactions(int page, int limit, String status, String dateFrom, String dateTo) {
        User shipperUser = getCurrentShipperUser();

        // Lay batch OPEN hien tai cua shipper
        PaymentSubmissionBatch openBatch = paymentSubmissionBatchRepository
                .findByShipperIdAndStatus(shipperUser.getId(), PaymentSubmissionBatchStatus.OPEN)
                .orElse(null);

        List<Map<String, Object>> transactions = new ArrayList<>();
        int totalCollected = 0;
        int totalPending = 0;
        int transactionCount = 0;

        if (openBatch != null) {
            List<PaymentSubmission> submissions = openBatch.getSubmissions();
            if (submissions == null) submissions = new ArrayList<>();

            // Loc theo ngay neu co
            if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
                LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
                LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
                submissions = submissions.stream()
                        .filter(ps -> ps.getPaidAt() != null
                                && !ps.getPaidAt().isBefore(from) && !ps.getPaidAt().isAfter(to))
                        .toList();
            }

            transactionCount = submissions.size();
            for (PaymentSubmission ps : submissions) {
                transactions.add(mapSubmission(ps));
                if (ps.getStatus() == PaymentSubmissionStatus.PENDING) {
                    int amt = ps.getActualAmount() != null ? ps.getActualAmount().intValue() : 0;
                    totalCollected += amt;
                    totalPending += amt;
                }
            }
        }

        Pagination pagination = new Pagination(transactionCount, page, limit,
                (int) Math.ceil((double) transactionCount / limit));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCollected", totalCollected);
        summary.put("totalSubmitted", 0);
        summary.put("totalPending", totalPending);
        summary.put("transactionCount", transactionCount);
        summary.put("openBatch", openBatch != null ? mapBatch(openBatch) : null);

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("pagination", pagination);
        result.put("summary", summary);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> collectCOD(CollectCODRequest request) {
        User shipperUser = getCurrentShipperUser();

        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
        for (PaymentSubmission ex : existing) {
            if (ex.getStatus() == PaymentSubmissionStatus.PENDING) {
                // Da co submission PENDING -> gan vao batch OPEN neu chua co
                PaymentSubmissionBatch batch = paymentSubmissionBatchRepository
                        .findByShipperIdAndStatus(shipperUser.getId(), PaymentSubmissionBatchStatus.OPEN)
                        .orElse(null);

                if (batch != null && ex.getBatch() == null) {
                    ex.setBatch(batch);
                    paymentSubmissionRepository.save(ex);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("submissionId", ex.getId());
                result.put("batchId", batch != null ? batch.getId() : null);
                result.put("amount", ex.getActualAmount() != null ? ex.getActualAmount().intValue()
                        : (ex.getSystemAmount() != null ? ex.getSystemAmount().intValue() : 0));
                return result;
            }
        }

        List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(order.getId());
        if (products == null) products = new ArrayList<>();

        BigDecimal codSum = BigDecimal.ZERO;
        List<PaymentSubmissionItem> items = new ArrayList<>();

        for (OrderProduct p : products) {
            int delivered = p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity();
            if (delivered <= 0) continue;

            BigDecimal unit = BigDecimal.valueOf(p.getPrice() == null ? 0 : p.getPrice());
            BigDecimal total = unit.multiply(BigDecimal.valueOf(delivered));
            codSum = codSum.add(total);

            PaymentSubmissionItem item = new PaymentSubmissionItem();
            item.setOrderProduct(p);
            item.setQuantity(delivered);
            item.setUnitAmount(unit);
            item.setTotalAmount(total);
            items.add(item);
        }

        int codAmount = codSum.intValue();

        if (codAmount <= 0) {
            throw new AppException(SettlementErrorCode.SETTLEMENT_NO_COD);
        }

        PaymentSubmission toSave = new PaymentSubmission();
        toSave.setOrder(order);
        toSave.setSystemAmount(codSum);
        toSave.setActualAmount(codSum);
        toSave.setStatus(PaymentSubmissionStatus.PENDING);
        toSave.setShipper(shipperUser);
        toSave.setNotes(request.getNotes());

        toSave = paymentSubmissionRepository.save(toSave);

        for (com.logistics.entity.PaymentSubmissionItem it : items) {
            it.setPaymentSubmission(toSave);
        }
        toSave.setItems(items);
        toSave = paymentSubmissionRepository.save(toSave);

        String code = "COD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + toSave.getId();
        toSave.setCode(code);
        PaymentSubmission submission = paymentSubmissionRepository.save(toSave);

        order.setCodStatus(OrderCodStatus.PENDING);
        orderRepository.save(order);

        // Tim hoac tao batch OPEN
        Employee employee = employeeRepository.findByUserId(shipperUser.getId())
                .stream().filter(e -> e.getOffice() != null).findFirst().orElse(null);
        Office office = employee != null ? employee.getOffice() : null;

        PaymentSubmissionBatch batch = paymentSubmissionBatchRepository
                .findByShipperIdAndStatus(shipperUser.getId(), PaymentSubmissionBatchStatus.OPEN)
                .orElse(null);

        if (batch == null) {
            batch = new PaymentSubmissionBatch();
            batch.setShipper(shipperUser);
            batch.setStatus(PaymentSubmissionBatchStatus.OPEN);
            batch.setTotalSystemAmount(BigDecimal.ZERO);
            batch.setTotalActualAmount(BigDecimal.ZERO);
            batch.setOffice(office);
            batch = paymentSubmissionBatchRepository.save(batch);

            String batchCode = "PSB" + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + batch.getId();
            batch.setCode(batchCode);
            batch = paymentSubmissionBatchRepository.save(batch);
        }

        // Gan submission vao batch
        submission.setBatch(batch);
        paymentSubmissionRepository.save(submission);

        if (batch.getSubmissions() == null) {
            batch.setSubmissions(new ArrayList<>());
        }
        boolean alreadyInBatch = batch.getSubmissions().stream()
                .anyMatch(s -> s.getId() != null && s.getId().equals(submission.getId()));
        if (!alreadyInBatch) {
            batch.getSubmissions().add(submission);
        }

        batch.setTotalSystemAmount(batch.getTotalSystemAmount().add(codSum));
        batch.setTotalActualAmount(batch.getTotalActualAmount().add(codSum));
        paymentSubmissionBatchRepository.save(batch);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("batchId", batch.getId());
        result.put("amount", codAmount);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitCOD(SubmitCODRequest request) {
        User shipperUser = getCurrentShipperUser();

        if (request.getBatchId() == null) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_QUANTITY);
        }

        PaymentSubmissionBatch batch = paymentSubmissionBatchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new AppException(SettlementErrorCode.SETTLEMENT_NO_SUBMISSION));

        if (!batch.getShipper().getId().equals(shipperUser.getId())) {
            throw new AppException(SettlementErrorCode.SETTLEMENT_ACCESS_DENIED);
        }

        if (batch.getStatus() != PaymentSubmissionBatchStatus.OPEN) {
            throw new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_INVALID_STATUS_CHANGE);
        }

        // Chuyen batch tu OPEN sang PROCESSING
        batch.setStatus(PaymentSubmissionBatchStatus.PROCESSING);
        batch.setNotes(request.getNotes());
        paymentSubmissionBatchRepository.save(batch);

        // Cap nhat tat ca submission trong batch
        List<PaymentSubmission> submissions = batch.getSubmissions();
        if (submissions != null) {
            for (PaymentSubmission sub : submissions) {
                sub.setStatus(PaymentSubmissionStatus.PROCESSING);
                paymentSubmissionRepository.save(sub);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batch.getId());
        result.put("batchCode", batch.getCode());
        result.put("submissionCount", submissions != null ? submissions.size() : 0);
        result.put("systemAmount", batch.getTotalSystemAmount());
        result.put("actualAmount", batch.getTotalActualAmount());

        return result;
    }

    @Override
    public Map<String, Object> getCODSubmissionHistory(int page, int limit, String status, String dateFrom, String dateTo) {
        User shipperUser = getCurrentShipperUser();
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "paidAt"));

        Specification<PaymentSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("shipper").get("id"), shipperUser.getId()));

            if (status != null && !status.isBlank()) {
                try {
                    PaymentSubmissionStatus statusEnum = PaymentSubmissionStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
                LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
                LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
                predicates.add(cb.between(root.get("paidAt"), from, to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PaymentSubmission> submissionPage = paymentSubmissionRepository.findAll(spec, pageable);

        List<PaymentSubmission> allSubmissions = paymentSubmissionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shipper").get("id"), shipperUser.getId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        int totalSubmitted = allSubmissions.stream()
                .mapToInt(ps -> ps.getActualAmount().intValue())
                .sum();

        int totalDiscrepancy = allSubmissions.stream()
                .mapToInt(ps -> ps.getActualAmount().subtract(ps.getSystemAmount()).intValue())
                .sum();

        int totalSubmissions = allSubmissions.size();

        List<Map<String, Object>> submissions = submissionPage.getContent().stream()
                .map(this::mapSubmission)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) submissionPage.getTotalElements(),
                page,
                limit,
                submissionPage.getTotalPages()
        );

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSubmitted", totalSubmitted);
        summary.put("totalDiscrepancy", totalDiscrepancy);
        summary.put("totalSubmissions", totalSubmissions);

        Map<String, Object> result = new HashMap<>();
        result.put("submissions", submissions);
        result.put("pagination", pagination);
        result.put("summary", summary);

        return result;
    }

    @Override
    public Map<String, Object> getCODBatchHistory(int page, int limit, String status, String dateFrom, String dateTo) {
        User shipperUser = getCurrentShipperUser();

        List<PaymentSubmissionBatchStatus> statusFilters = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            try {
                statusFilters.add(PaymentSubmissionBatchStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            statusFilters.add(PaymentSubmissionBatchStatus.PROCESSING);
            statusFilters.add(PaymentSubmissionBatchStatus.COMPLETED);
        }

        List<PaymentSubmissionBatch> allBatches = paymentSubmissionBatchRepository.findAll(
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("shipper").get("id"), shipperUser.getId()));
                    predicates.add(root.get("status").in(statusFilters));
                    if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
                        LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
                        LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
                        predicates.add(cb.between(root.get("createdAt"), from, to));
                    }
                    return cb.and(predicates.toArray(new Predicate[0]));
                });

        // Sort by createdAt DESC
        allBatches.sort((a, b) -> {
            LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return tb.compareTo(ta);
        });

        int total = allBatches.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);
        List<PaymentSubmissionBatch> pageBatches = fromIndex < total
                ? allBatches.subList(fromIndex, toIndex)
                : List.of();

        List<Map<String, Object>> batches = pageBatches.stream()
                .map(this::mapBatch)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(total, page, limit, (int) Math.ceil((double) total / limit));

        Map<String, Object> result = new HashMap<>();
        result.put("batches", batches);
        result.put("pagination", pagination);

        return result;
    }

    private Map<String, Object> mapSubmission(PaymentSubmission submission) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", submission.getId());
        map.put("code", submission.getCode());

        Order order = submission.getOrder();
        if (order != null) {
            map.put("orderId", order.getId());
            map.put("trackingNumber", order.getTrackingNumber());
        }

        PaymentSubmissionBatch batch = submission.getBatch();
        if (batch != null) {
            map.put("batchId", batch.getId());
            map.put("batchCode", batch.getCode());
        }

        map.put("systemAmount", submission.getSystemAmount() != null ? submission.getSystemAmount().intValue() : 0);
        map.put("actualAmount", submission.getActualAmount() != null ? submission.getActualAmount().intValue() : 0);
        map.put("discrepancy", submission.getActualAmount() != null && submission.getSystemAmount() != null
                ? submission.getActualAmount().subtract(submission.getSystemAmount()).intValue() : 0);
        map.put("status", submission.getStatus().name());
        map.put("notes", submission.getNotes());
        map.put("paidAt", submission.getPaidAt());
        map.put("checkedAt", submission.getCheckedAt());

        return map;
    }

    private Map<String, Object> mapBatch(PaymentSubmissionBatch batch) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", batch.getId());
        map.put("code", batch.getCode());
        map.put("status", batch.getStatus().name());
        map.put("totalSystemAmount", batch.getTotalSystemAmount() != null ? batch.getTotalSystemAmount().intValue() : 0);
        map.put("totalActualAmount", batch.getTotalActualAmount() != null ? batch.getTotalActualAmount().intValue() : 0);
        map.put("createdAt", batch.getCreatedAt());
        map.put("submissionCount", batch.getSubmissions() != null ? batch.getSubmissions().size() : 0);
        return map;
    }

    @Override
    public Map<String, Object> getCODBatchDetail(Long batchId) {
        User shipperUser = getCurrentShipperUser();

        PaymentSubmissionBatch batch = paymentSubmissionBatchRepository.findById(batchId.intValue())
                .orElseThrow(() -> new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_NOT_FOUND));

        if (batch.getShipper() == null || !batch.getShipper().getId().equals(shipperUser.getId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        Map<String, Object> batchMap = mapBatch(batch);

        List<PaymentSubmission> submissions = batch.getSubmissions();
        List<Map<String, Object>> submissionsList = submissions != null
                ? submissions.stream().map(this::mapSubmission).collect(Collectors.toList())
                : List.of();

        batchMap.put("submissions", submissionsList);

        return batchMap;
    }
}
