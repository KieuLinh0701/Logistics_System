package com.logistics.service.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.logistics.specification.PaymentSubmissonBatchSpecification;
import com.logistics.specification.PaymentSubmissonSpecification;
import com.logistics.response.Pagination;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.admin.AdminPaymentSubmissionListDto;
import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.mapper.PaymentSubmissionMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.admin.CreateBatchRequest;
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAdminService {

    private final PaymentSubmissionRepository submissionRepository;
    private final PaymentSubmissionBatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ApiResponse<ListResponse<AdminPaymentSubmissionListDto>> listSubmissions(String status) {
        try {
            List<PaymentSubmission> subs;
            if (status == null || status.isBlank()) {
                subs = submissionRepository.findAll();
            } else {
                PaymentSubmissionStatus s = PaymentSubmissionStatus.valueOf(status);
                subs = submissionRepository.findByStatus(s);
            }

                List<AdminPaymentSubmissionListDto> list = subs.stream()
                    .map(PaymentSubmissionMapper::toAdminDto)
                    .collect(Collectors.toList());

            ListResponse<AdminPaymentSubmissionListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(null);

            return new ApiResponse<>(true, "Lấy danh sách nộp tiền thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ListResponse<PaymentSubmissionBatch>> listBatches() {
        return listBatches(1, 50, null, null, null);
    }

    public ApiResponse<ListResponse<PaymentSubmissionBatch>> listBatches(int page, int limit, String search, String status, Integer shipperId) {
        try {
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, Sort.by("createdAt").descending());

            Specification<PaymentSubmissionBatch> spec = PaymentSubmissonBatchSpecification.unrestricted()
                    .and(PaymentSubmissonBatchSpecification.search(search))
                    .and(PaymentSubmissonBatchSpecification.status(status))
                    .and(PaymentSubmissonBatchSpecification.officeId(null));

            Page<PaymentSubmissionBatch> pageResult = batchRepository.findAll(spec, pageable);

            List<PaymentSubmissionBatch> list = pageResult.getContent();

            Pagination pagination = new Pagination((int) pageResult.getTotalElements(), page, limit, pageResult.getTotalPages());
            ListResponse<PaymentSubmissionBatch> resp = new ListResponse<>();
            resp.setList(list);
            resp.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách batch thành công", resp);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<Integer, List<AdminPaymentSubmissionListDto>>> listPendingGroupedByShipper() {
        try {
            List<PaymentSubmissionStatus> statuses = List.of(PaymentSubmissionStatus.PENDING);
            List<PaymentSubmission> subs = submissionRepository.findByBatchIsNullAndStatusIn(statuses);

            Map<Integer, List<AdminPaymentSubmissionListDto>> grouped = subs.stream()
                    .collect(Collectors.groupingBy(s -> s.getShipper().getId(),
                            Collectors.mapping(PaymentSubmissionMapper::toAdminDto, Collectors.toList())));

            return new ApiResponse<>(true, "Lấy danh sách COD chưa nộp theo shipper", grouped);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> processSubmission(Integer adminId, Integer submissionId, CreatePaymentSubmissionRequest form) {
        try {
            PaymentSubmission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Nộp tiền không tồn tại"));

            if (form.getActualAmount() != null) {
                submission.setActualAmount(form.getActualAmount());
            }

            if (!isBlank(form.getStatus())) {
                PaymentSubmissionStatus newStatus = PaymentSubmissionStatus.valueOf(form.getStatus());
                submission.setStatus(newStatus);
            }

            submission.setNotes(form.getNotes());
            submission.setCheckedAt(LocalDateTime.now());
            if (adminId != null) {
                userRepository.findById(adminId).ifPresent(submission::setCheckedBy);
            }

            submissionRepository.save(submission);
            return new ApiResponse<>(true, "Xử lý nộp tiền thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<PaymentSubmissionBatch> createBatch(Integer adminId, CreateBatchRequest req) {
        try {
            // Khóa các bản ghi nộp tiền để tránh tạo batch đồng thời
            List<PaymentSubmission> subs = submissionRepository.findByIdInForUpdate(req.getSubmissionIds());

            if (subs.isEmpty()) {
                return new ApiResponse<>(false, "Không có nộp tiền hợp lệ để tạo batch", null);
            }

            PaymentSubmissionBatch batch = new PaymentSubmissionBatch();
            userRepository.findById(req.getShipperId()).ifPresent(batch::setShipper);
            batch.setOffice(null);
            batch.setStatus(PaymentSubmissionBatchStatus.PENDING);

                BigDecimal totalSystem = subs.stream()
                    .map(PaymentSubmission::getSystemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalActual = subs.stream()
                    .map(PaymentSubmission::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            batch.setTotalSystemAmount(totalSystem);
            batch.setTotalActualAmount(totalActual);
            batch = batchRepository.save(batch);

            for (PaymentSubmission s : subs) {
                s.setBatch(batch);
                s.setStatus(PaymentSubmissionStatus.IN_BATCH);
            }
            submissionRepository.saveAll(subs);

            return new ApiResponse<>(true, "Tạo phiên đối soát thành công", batch);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<PaymentSubmissionBatch> getBatchById(Integer id) {
        try {
            PaymentSubmissionBatch batch = batchRepository.findById(id).orElse(null);
            if (batch == null) return new ApiResponse<>(false, "Phiên đối soát không tồn tại", null);
            return new ApiResponse<>(true, "OK", batch);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public byte[] exportBatches(int page, int limit, String search, String status, Integer shipperId) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Batches");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            Row header = sheet.createRow(0);
            String[] headers = new String[] { "Mã phiên", "Shipper", "Created At", "Total System Amount", "Total Actual Amount", "Status", "Notes" };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

                Specification<PaymentSubmissionBatch> spec = PaymentSubmissonBatchSpecification.unrestricted()
                    .and(PaymentSubmissonBatchSpecification.search(search))
                    .and(PaymentSubmissonBatchSpecification.status(status))
                    .and(PaymentSubmissonBatchSpecification.officeId(null));

                List<PaymentSubmissionBatch> batches = batchRepository.findAll(spec, Sort.by("createdAt").descending());

            int rowIdx = 1;
            for (PaymentSubmissionBatch b : batches) {
                Row row = sheet.createRow(rowIdx++);
                String shipper = b.getShipper() != null ? (b.getShipper().getLastName() + " " + b.getShipper().getFirstName()) : "";
                String created = b.getCreatedAt() != null ? dtf.format(b.getCreatedAt()) : "";

                row.createCell(0).setCellValue(b.getCode() != null ? b.getCode() : "");
                row.createCell(1).setCellValue(shipper);
                row.createCell(2).setCellValue(created);
                row.createCell(3).setCellValue(b.getTotalSystemAmount() != null ? b.getTotalSystemAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(b.getTotalActualAmount() != null ? b.getTotalActualAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(b.getStatus() != null ? b.getStatus().name() : "");
                row.createCell(6).setCellValue(b.getNotes() != null ? b.getNotes() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất batch xlsx: " + e.getMessage(), e);
        }
    }

    public byte[] exportSubmissions(String status, String search) {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Submissions");
                String[] headers = new String[] { "Code", "Order", "Shipper", "System Amount", "Actual Amount", "Status", "Paid At", "Checked At", "Notes" };
                Row header = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

                Specification<PaymentSubmission> spec = PaymentSubmissonSpecification.unrestricted()
                        .and(PaymentSubmissonSpecification.status(status))
                        .and(PaymentSubmissonSpecification.search(search));

                List<PaymentSubmission> submissions = submissionRepository.findAll(spec, Sort.by("paidAt").descending());

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                int rowIdx = 1;
                for (PaymentSubmission s : submissions) {
                    Row row = sheet.createRow(rowIdx++);
                    String orderCode = s.getOrder() != null ? s.getOrder().getTrackingNumber() : "";
                    String shipper = s.getShipper() != null ? (s.getShipper().getLastName() + " " + s.getShipper().getFirstName()) : "";
                    String paid = s.getPaidAt() != null ? dtf.format(s.getPaidAt()) : "";
                    String checked = s.getCheckedAt() != null ? dtf.format(s.getCheckedAt()) : "";

                    row.createCell(0).setCellValue(s.getCode() != null ? s.getCode() : "");
                    row.createCell(1).setCellValue(orderCode);
                    row.createCell(2).setCellValue(shipper);
                    Cell cellSys = row.createCell(3);
                    Cell cellAct = row.createCell(4);
                    if (s.getSystemAmount() != null) cellSys.setCellValue(s.getSystemAmount().doubleValue()); else cellSys.setCellValue(0);
                    if (s.getActualAmount() != null) cellAct.setCellValue(s.getActualAmount().doubleValue()); else cellAct.setCellValue(0);
                    row.createCell(5).setCellValue(s.getStatus() != null ? s.getStatus().name() : "");
                    row.createCell(6).setCellValue(paid);
                    row.createCell(7).setCellValue(checked);
                    row.createCell(8).setCellValue(s.getNotes() != null ? s.getNotes() : "");
                }

                for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    workbook.write(out);
                    return out.toByteArray();
                }
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi xuất submissions xlsx: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất submissions: " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\n")) {
            return "\"" + out + "\"";
        }
        return out;
    }

    @Transactional
    public ApiResponse<Boolean> completeBatch(Integer adminId, Integer batchId) {
        try {
            PaymentSubmissionBatch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new RuntimeException("Phiên đối soát không tồn tại"));

            // đánh dấu hoàn tất
            batch.setStatus(PaymentSubmissionBatchStatus.COMPLETED);
            batch.setCheckedAt(LocalDateTime.now());
            if (adminId != null) userRepository.findById(adminId).ifPresent(batch::setCheckedBy);

            // cập nhật trạng thái đơn hàng trong các nộp tiền
            if (batch.getSubmissions() != null) {
                for (PaymentSubmission s : batch.getSubmissions()) {
                    Order o = s.getOrder();
                    if (o != null) {
                        o.setCodStatus(OrderCodStatus.TRANSFERRED);
                        o.setPaymentStatus(OrderPaymentStatus.PAID);
                        o.setPaidAt(LocalDateTime.now());
                        orderRepository.save(o);
                    }
                }
            }

            batchRepository.save(batch);

            return new ApiResponse<>(true, "Hoàn tất phiên đối soát", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
