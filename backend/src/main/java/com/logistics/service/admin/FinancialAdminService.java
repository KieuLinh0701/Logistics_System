package com.logistics.service.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.PaymentErrorCode;
import com.logistics.mapper.PaymentSubmissionMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.financial.FinancialValidationService;
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ListResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAdminService {

    private final PaymentSubmissionRepository submissionRepository;
    private final PaymentSubmissionBatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FinancialValidationService financialValidationService;

    public ListResponse<AdminPaymentSubmissionListDto> listSubmissions(String status) {
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

        return data;
    }

    public ListResponse<PaymentSubmissionBatch> listBatches() {
        return listBatches(1, 50, null, null, null);
    }

    public ListResponse<PaymentSubmissionBatch> listBatches(int page, int limit, String search, String status, Integer shipperId) {
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

        return resp;
    }

    public Map<Integer, List<AdminPaymentSubmissionListDto>> listPendingGroupedByShipper() {
        List<PaymentSubmissionStatus> statuses = List.of(PaymentSubmissionStatus.PENDING);
        List<PaymentSubmission> subs = submissionRepository.findByBatchIsNullAndStatusIn(statuses);

        Map<Integer, List<AdminPaymentSubmissionListDto>> grouped = subs.stream()
                .collect(Collectors.groupingBy(s -> s.getShipper().getId(),
                        Collectors.mapping(PaymentSubmissionMapper::toAdminDto, Collectors.toList())));

        return grouped;
    }

    @Transactional
    public void processSubmission(Integer adminId, Integer submissionId, CreatePaymentSubmissionRequest form) {
        PaymentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_SUBMISSION_NOT_FOUND));

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
    }

    public PaymentSubmissionBatch getBatchById(Integer id) {
        PaymentSubmissionBatch batch = batchRepository.findById(id).orElse(null);
        if (batch == null) throw new AppException(PaymentErrorCode.PAYMENT_BATCH_NOT_FOUND);
        return batch;
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
            throw new AppException(PaymentErrorCode.PAYMENT_BATCH_EXPORT_ERROR);
        }
    }

    public byte[] exportSubmissions(String status, String search) {
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
            throw new AppException(PaymentErrorCode.PAYMENT_SUBMISSION_EXPORT_ERROR);
        }
    }

    @Transactional
    public void completeBatch(Integer adminId, Integer batchId) {
        PaymentSubmissionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_BATCH_NOT_FOUND));

        batch.setStatus(PaymentSubmissionBatchStatus.COMPLETED);
        batch.setCheckedAt(LocalDateTime.now());
        if (adminId != null) userRepository.findById(adminId).ifPresent(batch::setCheckedBy);

        if (batch.getSubmissions() != null) {
            for (PaymentSubmission s : batch.getSubmissions()) {
                Order o = s.getOrder();
                if (o != null) {
                    o.setCodStatus(OrderCodStatus.TRANSFERRED);
                    try {
                        try {
                            Optional<Order> locked = orderRepository.findByIdForUpdate(o.getId());
                            if (locked.isPresent()) {
                                financialValidationService.markOrderPaidIfEligible(locked.get());
                            } else {
                                financialValidationService.markOrderPaidIfEligible(o);
                            }
                        } catch (Exception lockEx) {
                            // Row-level lock failed, proceed without lock
                            financialValidationService.markOrderPaidIfEligible(o);
                        }
                    } catch (Exception ex) {
                        // Non-critical secondary error; order status already updated above
                        System.err.println("Error while validating payment for recipientaddress " + o.getId() + ": " + ex.getMessage());
                    }
                    orderRepository.save(o);
                }
            }
        }

        batchRepository.save(batch);

    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
