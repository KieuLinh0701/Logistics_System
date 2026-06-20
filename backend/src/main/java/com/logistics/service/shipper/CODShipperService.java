package com.logistics.service.shipper;

import com.logistics.entity.*;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.SettlementErrorCode;
import com.logistics.repository.*;
import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;
import com.logistics.response.Pagination;
import com.logistics.utils.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CODShipperService {

    @Autowired
    private PaymentSubmissionRepository paymentSubmissionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private PaymentSubmissionBatchRepository paymentSubmissionBatchRepository;

    private User getCurrentShipperUser() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.get(0).getUser();
    }

    public Map<String, Object> getCODTransactions(int page, int limit, String status, String dateFrom, String dateTo) {
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

        int totalCollected = paymentSubmissionRepository.findAll(spec)
                .stream()
                .filter(ps -> ps.getStatus() == PaymentSubmissionStatus.PENDING)
                .mapToInt(ps -> ps.getActualAmount().intValue())
                .sum();

        int totalSubmitted = paymentSubmissionRepository.findAll(spec)
                .stream()
                .filter(ps -> ps.getStatus() != PaymentSubmissionStatus.PENDING)
                .mapToInt(ps -> ps.getActualAmount().intValue())
                .sum();

        int totalPending = totalCollected;
        int transactionCount = (int) submissionPage.getTotalElements();

        List<Map<String, Object>> transactions = submissionPage.getContent().stream()
                .map(this::mapSubmission)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) submissionPage.getTotalElements(),
                page,
                limit,
                submissionPage.getTotalPages()
        );

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCollected", totalCollected);
        summary.put("totalSubmitted", totalSubmitted);
        summary.put("totalPending", totalPending);
        summary.put("transactionCount", transactionCount);

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("pagination", pagination);
        result.put("summary", summary);

        return result;
    }

    @Transactional
    public Map<String, Object> collectCOD(CollectCODRequest request) {
        User shipperUser = getCurrentShipperUser();

        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
        for (PaymentSubmission ex : existing) {
            if (ex.getStatus() == PaymentSubmissionStatus.PENDING) {
                Map<String, Object> result = new HashMap<>();
                result.put("submissionId", ex.getId());
                result.put("amount", ex.getActualAmount() != null ? ex.getActualAmount().intValue() : (ex.getSystemAmount() != null ? ex.getSystemAmount().intValue() : 0));
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

        PaymentSubmission submission = new PaymentSubmission();
        submission.setOrder(order);
        submission.setSystemAmount(codSum);
        submission.setActualAmount(codSum);
        submission.setStatus(PaymentSubmissionStatus.PENDING);
        submission.setShipper(shipperUser);
        submission.setNotes(request.getNotes());

        submission = paymentSubmissionRepository.save(submission);

        for (com.logistics.entity.PaymentSubmissionItem it : items) {
            it.setPaymentSubmission(submission);
        }
        submission.setItems(items);
        submission = paymentSubmissionRepository.save(submission);

        String code = "COD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
        submission.setCode(code);
        paymentSubmissionRepository.save(submission);

        order.setCodStatus(OrderCodStatus.PENDING);
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("amount", codAmount);

        return result;
    }

    @Transactional
    public Map<String, Object> submitCOD(SubmitCODRequest request) {
        User shipperUser = getCurrentShipperUser();

        if (request.getTotalAmount() == null || request.getTotalAmount() <= 0) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_QUANTITY);
        }

        PaymentSubmissionBatch batch =
                paymentSubmissionBatchRepository.findByShipperIdAndStatus(
                        shipperUser.getId(),
                        PaymentSubmissionBatchStatus.OPEN)
                .orElseThrow(() -> new AppException(SettlementErrorCode.SETTLEMENT_NO_OPEN_BATCH));

        if (batch.getSubmissions() == null || batch.getSubmissions().isEmpty()) {
            throw new AppException(SettlementErrorCode.SETTLEMENT_NO_SUBMISSION);
        }

        BigDecimal provided = BigDecimal.valueOf(request.getTotalAmount());
        BigDecimal expected = batch.getTotalSystemAmount();

        if (provided.compareTo(expected) > 0) {
            throw new AppException(SettlementErrorCode.SETTLEMENT_AMOUNT_EXCEEDED);
        }

        for (PaymentSubmission submission : batch.getSubmissions()) {
            submission.setStatus(PaymentSubmissionStatus.PROCESSING);
            submission.setPaidAt(LocalDateTime.now());
            paymentSubmissionRepository.save(submission);
        }

        batch.setNotes(request.getNotes());
        batch.setTotalActualAmount(provided);
        batch.setStatus(PaymentSubmissionBatchStatus.PROCESSING);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionCount", batch.getSubmissions().size());
        result.put("systemAmount", batch.getTotalSystemAmount());
        result.put("actualAmount", batch.getTotalActualAmount());
        result.put("discrepancy", provided.subtract(expected).intValue());

        return result;
    }

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

    private Map<String, Object> mapSubmission(PaymentSubmission submission) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", submission.getId());
        map.put("code", submission.getCode());
        
        Order order = submission.getOrder();
        if (order != null) {
            map.put("orderId", order.getId());
            map.put("trackingNumber", order.getTrackingNumber());
        }
        
        map.put("systemAmount", submission.getSystemAmount().intValue());
        map.put("actualAmount", submission.getActualAmount().intValue());
        map.put("discrepancy", submission.getActualAmount().subtract(submission.getSystemAmount()).intValue());
        map.put("status", submission.getStatus().name());
        map.put("notes", submission.getNotes());
        map.put("paidAt", submission.getPaidAt());
        map.put("checkedAt", submission.getCheckedAt());
        
        return map;
    }
}
