package com.logistics.service.shipper;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionItem;
import com.logistics.entity.User;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.OrderProductRepository;
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
import com.logistics.entity.OrderProduct;

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

            // Khóa đơn để tránh tạo submission đồng thời
            Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Kiểm tra đã có PaymentSubmission COD đang chờ cho đơn này chưa (idempotency)
            List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
            for (PaymentSubmission ex : existing) {
                if (ex.getStatus() == PaymentSubmissionStatus.PENDING || ex.getStatus() == PaymentSubmissionStatus.IN_BATCH) {
                    // trả về thông tin submission hiện có thay vì tạo bản ghi trùng
                    Map<String, Object> result = new HashMap<>();
                    result.put("submissionId", ex.getId());
                    result.put("amount", ex.getActualAmount() != null ? ex.getActualAmount().intValue() : (ex.getSystemAmount() != null ? ex.getSystemAmount().intValue() : 0));
                    return new ApiResponse<>(true, "Submission đã tồn tại, trả về bản ghi hiện có", result);
                }
            }

            // Tính COD dựa trên số lượng đã giao và giá đơn vị (tính toán hệ thống)
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
                return new ApiResponse<>(false, "Không có COD hợp lệ để thu (không có sản phẩm đã giao)", null);
            }

            PaymentSubmission submission = new PaymentSubmission();
            submission.setOrder(order);
            submission.setSystemAmount(codSum);
            submission.setActualAmount(codSum);
            submission.setStatus(PaymentSubmissionStatus.PENDING);
            submission.setShipper(shipperUser);
            submission.setNotes(request.getNotes());

            // Lưu bản ghi đối soát trước để có được id
            submission = paymentSubmissionRepository.save(submission);

            // Gắn các mục (items) vào và lưu lại
            for (com.logistics.entity.PaymentSubmissionItem it : items) {
                it.setPaymentSubmission(submission);
            }
            submission.setItems(items);
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

            return new ApiResponse<>(true, "Thu tiền COD thành công (hệ thống tính tự động)", result);
        } catch (Exception e) {
            throw e;
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

            // Lấy các submission theo ids và lock để tránh race
            List<PaymentSubmission> submissions = new ArrayList<>();
            List<Integer> ids = new ArrayList<>(request.getTransactionIds());
            List<PaymentSubmission> lockedSubs = paymentSubmissionRepository.findByIdInForUpdate(ids);
            BigDecimal expectedTotal = BigDecimal.ZERO;
            for (PaymentSubmission submission : lockedSubs) {
                if (submission == null) continue;
                if (submission.getShipper() == null || !Objects.equals(submission.getShipper().getId(), shipperUser.getId())) continue;
                if (submission.getStatus() != PaymentSubmissionStatus.PENDING && submission.getStatus() != PaymentSubmissionStatus.IN_BATCH) continue;
                submissions.add(submission);
                BigDecimal sys = submission.getSystemAmount() != null ? submission.getSystemAmount() : BigDecimal.ZERO;
                expectedTotal = expectedTotal.add(sys);
            }

            if (submissions.isEmpty()) {
                return new ApiResponse<>(false, "Không có đối soát COD hợp lệ để nộp", null);
            }

            BigDecimal provided = BigDecimal.valueOf(request.getTotalAmount());
            if (provided.compareTo(expectedTotal) != 0) {
                // Từ chối — backend tính lại tổng mong đợi và không tin dữ liệu từ frontend
                String msg = String.format("Tổng tiền nộp không khớp: client=%s, expected=%s", provided.toPlainString(), expectedTotal.toPlainString());
                return new ApiResponse<>(false, msg, null);
            }

            // Đánh dấu các submission là IN_BATCH và đặt actualAmount = systemAmount (server là nguồn dữ liệu chính xác)
            for (PaymentSubmission submission : submissions) {
                BigDecimal sys = submission.getSystemAmount() != null ? submission.getSystemAmount() : BigDecimal.ZERO;
                submission.setActualAmount(sys);
                submission.setStatus(PaymentSubmissionStatus.IN_BATCH);
                submission.setNotes(request.getNotes());
                submission.setPaidAt(LocalDateTime.now());
                paymentSubmissionRepository.save(submission);
            }

            // Sử dụng expectedTotal đã tính ở trên làm tổng hệ thống chính thức
            BigDecimal systemTotalAmount = expectedTotal;

            Map<String, Object> result = new HashMap<>();
            result.put("submissionCount", submissions.size());
            result.put("systemAmount", systemTotalAmount);
            result.put("actualAmount", request.getTotalAmount());
            // sai lệch = provided - systemTotalAmount
            result.put("discrepancy", provided.subtract(systemTotalAmount).intValue());

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
