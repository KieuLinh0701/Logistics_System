package com.logistics.service.shipper;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.User;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CODShipperService {

    @Autowired
    private PaymentSubmissionRepository paymentSubmissionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User getCurrentShipperUser() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin nhân viên (shipper)");
        }
        return employees.get(0).getUser();
    }

    // Lấy danh sách đối soát COD (PaymentSubmission) của shipper
    public ApiResponse<Map<String, Object>> getCODTransactions(int page, int limit, String status, String dateFrom, String dateTo) {
        try {
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
                    .filter(ps -> ps.getStatus() == PaymentSubmissionStatus.PENDING || ps.getStatus() == PaymentSubmissionStatus.IN_BATCH)
                    .mapToInt(ps -> ps.getActualAmount().intValue())
                    .sum();

            int totalSubmitted = paymentSubmissionRepository.findAll(spec)
                    .stream()
                    .filter(ps -> ps.getStatus() != PaymentSubmissionStatus.PENDING && ps.getStatus() != PaymentSubmissionStatus.IN_BATCH)
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

            return new ApiResponse<>(true, "Lấy danh sách đối soát COD thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    // Thu tiền COD (có thể được gọi từ OrderShipperService khi giao hàng thành công)
    @Transactional
    public ApiResponse<Map<String, Object>> collectCOD(CollectCODRequest request) {
        try {
            User shipperUser = getCurrentShipperUser();
            
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra đã có PaymentSubmission COD đang chờ cho đơn này chưa
            boolean alreadyExists = paymentSubmissionRepository.findByOrderId(order.getId())
                    .stream()
                    .anyMatch(ps -> ps.getStatus() == PaymentSubmissionStatus.PENDING || ps.getStatus() == PaymentSubmissionStatus.IN_BATCH);

            if (alreadyExists) {
                return new ApiResponse<>(false, "Đã thu hoặc đang đối soát COD cho đơn hàng này", null);
            }

            int codAmount = (request.getActualAmount() != null && request.getActualAmount() > 0)
                    ? request.getActualAmount()
                    : order.getCod();

            PaymentSubmission submission = new PaymentSubmission();
            submission.setOrder(order);
            submission.setSystemAmount(BigDecimal.valueOf(codAmount));
            submission.setActualAmount(BigDecimal.valueOf(codAmount));
            submission.setStatus(PaymentSubmissionStatus.PENDING);
            submission.setShipper(shipperUser);
            submission.setNotes(request.getNotes());
            submission.setPaidAt(LocalDateTime.now());

            submission = paymentSubmissionRepository.save(submission);
            String code = "COD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
            submission.setCode(code);
            paymentSubmissionRepository.save(submission);

            // Cập nhật codStatus → PENDING (shipper đang giữ tiền)
            order.setCodStatus(OrderCodStatus.PENDING);
            orderRepository.save(order);

            Map<String, Object> result = new HashMap<>();
            result.put("submissionId", submission.getId());
            result.put("amount", codAmount);

            return new ApiResponse<>(true, "Thu tiền COD thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    // Nộp tiền COD về công ty
    @Transactional
    public ApiResponse<Map<String, Object>> submitCOD(SubmitCODRequest request) {
        try {
            User shipperUser = getCurrentShipperUser();

            if (request.getTransactionIds() == null || request.getTransactionIds().isEmpty()) {
                return new ApiResponse<>(false, "Thiếu danh sách đối soát COD", null);
            }

            if (request.getTotalAmount() == null || request.getTotalAmount() <= 0) {
                return new ApiResponse<>(false, "Số tiền nộp không hợp lệ", null);
            }

            // Lấy các submission theo ids
            List<PaymentSubmission> submissions = new ArrayList<>();
            int systemTotalAmount = 0;
            for (Integer submissionId : request.getTransactionIds()) {
                PaymentSubmission submission = paymentSubmissionRepository.findById(submissionId).orElse(null);
                if (submission == null) continue;
                if (submission.getShipper() == null || !Objects.equals(submission.getShipper().getId(), shipperUser.getId())) continue;
                if (submission.getStatus() != PaymentSubmissionStatus.PENDING && submission.getStatus() != PaymentSubmissionStatus.IN_BATCH) continue;
                submissions.add(submission);
                systemTotalAmount += submission.getActualAmount().intValue();
            }

            if (submissions.isEmpty()) {
                return new ApiResponse<>(false, "Không có đối soát COD hợp lệ để nộp", null);
            }

            BigDecimal remainingAmount = BigDecimal.valueOf(request.getTotalAmount());
            for (int i = 0; i < submissions.size(); i++) {
                PaymentSubmission submission = submissions.get(i);

                BigDecimal actualAmount;
                if (i == submissions.size() - 1) {
                    actualAmount = remainingAmount;
                } else {
                    actualAmount = BigDecimal.valueOf(request.getTotalAmount())
                            .multiply(submission.getActualAmount())
                            .divide(BigDecimal.valueOf(systemTotalAmount), 2, BigDecimal.ROUND_HALF_UP);
                    remainingAmount = remainingAmount.subtract(actualAmount);
                }

                submission.setActualAmount(actualAmount);
                submission.setStatus(PaymentSubmissionStatus.IN_BATCH);
                submission.setNotes(request.getNotes());
                submission.setPaidAt(LocalDateTime.now());
                paymentSubmissionRepository.save(submission);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("submissionCount", submissions.size());
            result.put("systemAmount", systemTotalAmount);
            result.put("actualAmount", request.getTotalAmount());
            result.put("discrepancy", request.getTotalAmount() - systemTotalAmount);

            return new ApiResponse<>(true, "Nộp tiền COD thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    // Lấy lịch sử nộp tiền COD
    public ApiResponse<Map<String, Object>> getCODSubmissionHistory(int page, int limit, String status, String dateFrom, String dateTo) {
        try {
            User shipperUser = getCurrentShipperUser();
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "paidAt"));

            Specification<PaymentSubmission> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Chỉ lấy PaymentSubmission của shipper này
                predicates.add(cb.equal(root.get("shipper").get("id"), shipperUser.getId()));

                // Filter theo status
                if (status != null && !status.isBlank()) {
                    try {
                        PaymentSubmissionStatus statusEnum = PaymentSubmissionStatus.valueOf(status.toUpperCase());
                        predicates.add(cb.equal(root.get("status"), statusEnum));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                // Filter theo date range
                if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
                    LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
                    LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
                    predicates.add(cb.between(root.get("paidAt"), from, to));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<PaymentSubmission> submissionPage = paymentSubmissionRepository.findAll(spec, pageable);

            // Tính toán summary
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

            // Map submissions
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

            return new ApiResponse<>(true, "Lấy lịch sử nộp tiền COD thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    // Helper methods
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
