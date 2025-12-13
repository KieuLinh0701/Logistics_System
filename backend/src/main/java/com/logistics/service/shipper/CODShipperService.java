package com.logistics.service.shipper;

import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.repository.*;
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
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentSubmissionRepository paymentSubmissionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User getCurrentShipperUser() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên (shipper)"));
        return employee.getUser();
    }

    // Lấy danh sách giao dịch COD của shipper
    public ApiResponse<Map<String, Object>> getCODTransactions(int page, int limit, String status, String dateFrom, String dateTo) {
        try {
            User shipperUser = getCurrentShipperUser();
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "paidAt"));

            Specification<Transaction> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Chỉ lấy transaction COD của shipper này
                predicates.add(cb.equal(root.get("purpose"), TransactionPurpose.COD_RETURN));
                predicates.add(cb.equal(root.get("collectedBy").get("id"), shipperUser.getId()));

                // Filter theo status
                if (status != null && !status.isBlank()) {
                    try {
                        TransactionStatus statusEnum = TransactionStatus.valueOf(status.toUpperCase());
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

            Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

            // Tính toán summary
            List<Transaction> allTransactions = transactionRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("purpose"), TransactionPurpose.COD_RETURN));
                predicates.add(cb.equal(root.get("collectedBy").get("id"), shipperUser.getId()));
                return cb.and(predicates.toArray(new Predicate[0]));
            });

            int totalCollected = allTransactions.stream()
                    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                    .mapToInt(Transaction::getAmount)
                    .sum();

            // Tính tổng đã nộp: lấy từ PaymentSubmission có status != PENDING
            int totalSubmitted = paymentSubmissionRepository.findAll()
                    .stream()
                    .filter(ps -> {
                        Transaction t = ps.getTransaction();
                        return t != null 
                                && t.getPurpose() == TransactionPurpose.COD_RETURN
                                && Objects.equals(t.getCollectedBy().getId(), shipperUser.getId())
                                && ps.getStatus() != PaymentSubmissionStatus.PENDING;
                    })
                    .mapToInt(ps -> ps.getActualAmount().intValue())
                    .sum();

            int totalPending = totalCollected - totalSubmitted;
            int transactionCount = allTransactions.size();

            // Map transactions
            List<Map<String, Object>> transactions = transactionPage.getContent().stream()
                    .map(this::mapTransaction)
                    .collect(Collectors.toList());

            Pagination pagination = new Pagination(
                    (int) transactionPage.getTotalElements(),
                    page,
                    limit,
                    transactionPage.getTotalPages()
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

            return new ApiResponse<>(true, "Lấy danh sách giao dịch COD thành công", result);
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

            // Kiểm tra xem đã có transaction COD cho đơn này chưa
            boolean alreadyExists = transactionRepository.findByOrderId(order.getId())
                    .stream()
                    .anyMatch(t -> t.getPurpose() == TransactionPurpose.COD_RETURN
                            && t.getStatus() == TransactionStatus.SUCCESS
                            && t.getCollectedBy() != null
                            && Objects.equals(t.getCollectedBy().getId(), shipperUser.getId()));

            if (alreadyExists) {
                return new ApiResponse<>(false, "Đã thu COD cho đơn hàng này", null);
            }

            // Sử dụng actualAmount nếu có, nếu không dùng cod từ order
            int codAmount = (request.getActualAmount() != null && request.getActualAmount() > 0) 
                    ? request.getActualAmount() 
                    : order.getCod();

            Transaction transaction = new Transaction();
            transaction.setOrder(order);
            transaction.setCollectedBy(shipperUser);
            transaction.setTitle("Thu tiền COD đơn hàng " + order.getTrackingNumber());
            transaction.setAmount(codAmount);
            transaction.setType(TransactionType.INCOME);
            transaction.setMethod(TransactionMethod.CASH);
            transaction.setPurpose(TransactionPurpose.COD_RETURN);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setPaidAt(LocalDateTime.now());
            transaction.setNotes(request.getNotes());

            transaction = transactionRepository.save(transaction);

            // Tạo mã transaction
            String code = "TRANS_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + transaction.getId();
            transaction.setCode(code);
            transactionRepository.save(transaction);

            Map<String, Object> result = new HashMap<>();
            result.put("transactionId", transaction.getId());
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
                return new ApiResponse<>(false, "Thiếu danh sách giao dịch COD", null);
            }

            if (request.getTotalAmount() == null || request.getTotalAmount() <= 0) {
                return new ApiResponse<>(false, "Số tiền nộp không hợp lệ", null);
            }

            // Lấy các transaction
            List<Transaction> transactions = new ArrayList<>();
            int systemTotalAmount = 0;
            
            for (Integer transactionId : request.getTransactionIds()) {
                Transaction transaction = transactionRepository.findById(transactionId)
                        .orElse(null);
                
                if (transaction == null) {
                    continue;
                }

                // Kiểm tra transaction thuộc về shipper này
                if (transaction.getCollectedBy() == null 
                        || !Objects.equals(transaction.getCollectedBy().getId(), shipperUser.getId())) {
                    continue;
                }

                // Kiểm tra transaction là COD_RETURN
                if (transaction.getPurpose() != TransactionPurpose.COD_RETURN) {
                    continue;
                }

                // Kiểm tra transaction chưa có PaymentSubmission
                List<PaymentSubmission> existingSubmissions = paymentSubmissionRepository.findByTransactionId(transactionId);
                if (!existingSubmissions.isEmpty()) {
                    continue; // Đã nộp rồi, bỏ qua
                }

                transactions.add(transaction);
                systemTotalAmount += transaction.getAmount();
            }

            if (transactions.isEmpty()) {
                return new ApiResponse<>(false, "Không có giao dịch COD hợp lệ để nộp", null);
            }

            // Tạo PaymentSubmission cho mỗi transaction
            List<PaymentSubmission> submissions = new ArrayList<>();
            BigDecimal remainingAmount = BigDecimal.valueOf(request.getTotalAmount());
            
            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                PaymentSubmission submission = new PaymentSubmission();
                submission.setOrder(transaction.getOrder());
                submission.setTransaction(transaction);
                submission.setSystemAmount(BigDecimal.valueOf(transaction.getAmount()));
                
                // Tính actualAmount: chia đều hoặc theo tỷ lệ
                BigDecimal actualAmount;
                if (i == transactions.size() - 1) {
                    // Transaction cuối cùng: lấy phần còn lại để tránh sai số
                    actualAmount = remainingAmount;
                } else {
                    // Tính theo tỷ lệ
                    actualAmount = BigDecimal.valueOf(request.getTotalAmount())
                            .multiply(BigDecimal.valueOf(transaction.getAmount()))
                            .divide(BigDecimal.valueOf(systemTotalAmount), 2, BigDecimal.ROUND_HALF_UP);
                    remainingAmount = remainingAmount.subtract(actualAmount);
                }
                
                submission.setActualAmount(actualAmount);
                submission.setStatus(PaymentSubmissionStatus.PENDING);
                submission.setNotes(request.getNotes());
                submission.setPaidAt(LocalDateTime.now());

                submission = paymentSubmissionRepository.save(submission);

                // Tạo mã submission sau khi có ID
                String code = "COD_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
                submission.setCode(code);
                submission = paymentSubmissionRepository.save(submission);

                submissions.add(submission);
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
                
                // Chỉ lấy PaymentSubmission của transactions COD của shipper này
                predicates.add(cb.equal(root.get("transaction").get("purpose"), TransactionPurpose.COD_RETURN));
                predicates.add(cb.equal(root.get("transaction").get("collectedBy").get("id"), shipperUser.getId()));

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
                predicates.add(cb.equal(root.get("transaction").get("purpose"), TransactionPurpose.COD_RETURN));
                predicates.add(cb.equal(root.get("transaction").get("collectedBy").get("id"), shipperUser.getId()));
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
    private Map<String, Object> mapTransaction(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transaction.getId());
        map.put("code", transaction.getCode());
        
        Order order = transaction.getOrder();
        if (order != null) {
            map.put("orderId", order.getId());
            map.put("trackingNumber", order.getTrackingNumber());
            map.put("recipientName", order.getRecipientName());
            map.put("recipientPhone", order.getRecipientPhone());
        }
        
        map.put("codAmount", transaction.getAmount());
        map.put("status", transaction.getStatus().name());
        map.put("collectedAt", transaction.getPaidAt());
        map.put("notes", transaction.getNotes());
        
        return map;
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
