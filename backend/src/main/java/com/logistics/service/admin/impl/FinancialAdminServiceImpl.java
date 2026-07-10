package com.logistics.service.admin.impl;

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
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.admin.FinancialAdminService;
import com.logistics.service.financial.FinancialValidationService;
import com.logistics.specification.PaymentSubmissonBatchSpecification;
import com.logistics.specification.PaymentSubmissonSpecification;
import com.logistics.utils.ExcelExportHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialAdminServiceImpl implements FinancialAdminService {

    private final PaymentSubmissionRepository submissionRepository;
    private final PaymentSubmissionBatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FinancialValidationService financialValidationService;

    @Override
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

    @Override
    public ListResponse<PaymentSubmissionBatch> listBatches() {
        return listBatches(1, 50, null, null, null);
    }

    @Override
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

    @Override
    public Map<Integer, List<AdminPaymentSubmissionListDto>> listPendingGroupedByShipper() {
        List<PaymentSubmissionStatus> statuses = List.of(PaymentSubmissionStatus.PENDING);
        List<PaymentSubmission> subs = submissionRepository.findByBatchIsNullAndStatusIn(statuses);

        Map<Integer, List<AdminPaymentSubmissionListDto>> grouped = subs.stream()
                .collect(Collectors.groupingBy(s -> s.getShipper().getId(),
                        Collectors.mapping(PaymentSubmissionMapper::toAdminDto, Collectors.toList())));

        return grouped;
    }

    @Override
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

    @Override
    public PaymentSubmissionBatch getBatchById(Integer id) {
        PaymentSubmissionBatch batch = batchRepository.findById(id).orElse(null);
        if (batch == null) throw new AppException(PaymentErrorCode.PAYMENT_BATCH_NOT_FOUND);
        return batch;
    }

    @Override
    public byte[] exportBatches(int page, int limit, String search, String status, Integer shipperId) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo phiên đối soát");

            List<String> headers = Arrays.asList(
                    "Mã phiên", "Shipper", "Ngày tạo", "Tổng số tiền hệ thống (VND)",
                    "Tổng số tiền thực tế (VND)", "Trạng thái", "Ghi chú");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo phiên đối soát",
                    java.time.LocalDate.now(), java.time.LocalDate.now(), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle dateTimeStyle = ExcelExportHelper.createDateStyle(workbook);
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            Specification<PaymentSubmissionBatch> spec = PaymentSubmissonBatchSpecification.unrestricted()
                    .and(PaymentSubmissonBatchSpecification.search(search))
                    .and(PaymentSubmissonBatchSpecification.status(status))
                    .and(PaymentSubmissonBatchSpecification.officeId(null));

            List<PaymentSubmissionBatch> batches = batchRepository.findAll(spec, Sort.by("createdAt").descending());

            for (PaymentSubmissionBatch b : batches) {
                Row row = sheet.createRow(rowIdx++);
                String shipper = b.getShipper() != null ? (b.getShipper().getLastName() + " " + b.getShipper().getFirstName()) : "";

                ExcelExportHelper.writeTextCell(row, 0, b.getCode() == null ? "" : b.getCode(), textStyle);
                ExcelExportHelper.writeTextCell(row, 1, shipper, textStyle);

                org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(2);
                if (b.getCreatedAt() != null) {
                    dateCell.setCellValue(b.getCreatedAt());
                    dateCell.setCellStyle(dateTimeStyle);
                } else {
                    dateCell.setBlank();
                }

                org.apache.poi.ss.usermodel.Cell sysCell = row.createCell(3);
                if (b.getTotalSystemAmount() != null) {
                    sysCell.setCellValue(b.getTotalSystemAmount().doubleValue());
                } else {
                    sysCell.setCellValue(0);
                }
                sysCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell actCell = row.createCell(4);
                if (b.getTotalActualAmount() != null) {
                    actCell.setCellValue(b.getTotalActualAmount().doubleValue());
                } else {
                    actCell.setCellValue(0);
                }
                actCell.setCellStyle(currencyStyle);

                ExcelExportHelper.writeTextCell(row, 5,
                        b.getStatus() != null ? translatePaymentSubmissionBatchStatus(b.getStatus()) : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 6, b.getNotes() == null ? "" : b.getNotes(), textStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(PaymentErrorCode.PAYMENT_BATCH_EXPORT_ERROR);
        }
    }

    @Override
    public byte[] exportSubmissions(String status, String search) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo phiếu đối soát");

            List<String> headers = Arrays.asList(
                    "Mã phiếu", "Mã vận đơn", "Shipper",
                    "Số tiền hệ thống (VND)", "Số tiền thực tế (VND)",
                    "Trạng thái", "Ngày nộp", "Ngày đối soát", "Ghi chú");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo phiếu đối soát",
                    java.time.LocalDate.now(), java.time.LocalDate.now(), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle dateTimeStyle = ExcelExportHelper.createDateStyle(workbook);
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            Specification<PaymentSubmission> spec = PaymentSubmissonSpecification.unrestricted()
                    .and(PaymentSubmissonSpecification.status(status))
                    .and(PaymentSubmissonSpecification.search(search));

            List<PaymentSubmission> submissions = submissionRepository.findAll(spec, Sort.by("paidAt").descending());

            for (PaymentSubmission s : submissions) {
                Row row = sheet.createRow(rowIdx++);
                String orderCode = s.getOrder() != null ? s.getOrder().getTrackingNumber() : "";
                String shipper = s.getShipper() != null ? (s.getShipper().getLastName() + " " + s.getShipper().getFirstName()) : "";

                ExcelExportHelper.writeTextCell(row, 0, s.getCode() == null ? "" : s.getCode(), textStyle);
                ExcelExportHelper.writeTextCell(row, 1, orderCode, textStyle);
                ExcelExportHelper.writeTextCell(row, 2, shipper, textStyle);

                org.apache.poi.ss.usermodel.Cell sysCell = row.createCell(3);
                if (s.getSystemAmount() != null) {
                    sysCell.setCellValue(s.getSystemAmount().doubleValue());
                } else {
                    sysCell.setCellValue(0);
                }
                sysCell.setCellStyle(currencyStyle);

                org.apache.poi.ss.usermodel.Cell actCell = row.createCell(4);
                if (s.getActualAmount() != null) {
                    actCell.setCellValue(s.getActualAmount().doubleValue());
                } else {
                    actCell.setCellValue(0);
                }
                actCell.setCellStyle(currencyStyle);

                ExcelExportHelper.writeTextCell(row, 5,
                        s.getStatus() != null ? translatePaymentSubmissionStatus(s.getStatus()) : "", textStyle);

                org.apache.poi.ss.usermodel.Cell paidCell = row.createCell(6);
                if (s.getPaidAt() != null) {
                    paidCell.setCellValue(s.getPaidAt());
                    paidCell.setCellStyle(dateTimeStyle);
                } else {
                    paidCell.setBlank();
                }

                org.apache.poi.ss.usermodel.Cell checkedCell = row.createCell(7);
                if (s.getCheckedAt() != null) {
                    checkedCell.setCellValue(s.getCheckedAt());
                    checkedCell.setCellStyle(dateTimeStyle);
                } else {
                    checkedCell.setBlank();
                }

                ExcelExportHelper.writeTextCell(row, 8, s.getNotes() == null ? "" : s.getNotes(), textStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(PaymentErrorCode.PAYMENT_SUBMISSION_EXPORT_ERROR);
        }
    }

    private static String translatePaymentSubmissionBatchStatus(PaymentSubmissionBatchStatus status) {
        if (status == null) return "";
        return switch (status) {
            case OPEN -> "Đang mở";
            case PROCESSING -> "Đang xử lý";
            case COMPLETED -> "Đã hoàn thành";
        };
    }

    private static String translatePaymentSubmissionStatus(PaymentSubmissionStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Shipper đang giữ tiền";
            case PROCESSING -> "Manager đang xem xét";
            case MATCHED -> "Đã khớp";
            case MISMATCHED -> "Lệch - chờ xử lý";
            case ADJUSTED -> "Đã chốt";
        };
    }

    @Override
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
                            financialValidationService.markOrderPaidIfEligible(o);
                        }
                    } catch (Exception ex) {
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
