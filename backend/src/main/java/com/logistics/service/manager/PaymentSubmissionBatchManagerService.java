package com.logistics.service.manager;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.entity.*;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.PaymentSubmissionBatchErrorCode;
import com.logistics.mapper.PaymentSubmissionBatchMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchEditForm;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.PaymentSubmissonBatchSpecification;
import com.logistics.utils.PaymentSubmissionBatchUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentSubmissionBatchManagerService {

    private final EmployeeManagerService employeeManagerService;

    private final PaymentSubmissionBatchRepository batchRepository;

    private final PaymentSubmissionRepository submissionRepository;

    private final NotificationService notificationService;

    private final OrderRepository orderRepository;

    public ListResponse<ManagerPaymentSubmissionBatchListDto> list(
            Integer userId, SearchRequest request) {
            Sort sort = buildSort(request.getSort());
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
            Page<PaymentSubmissionBatch> pageData = getPaymentSubmissionBatchPage(userId, request, pageable);

            List<ManagerPaymentSubmissionBatchListDto> list = pageData.getContent()
                    .stream()
                    .map(PaymentSubmissionBatchMapper::toDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) pageData.getTotalElements(),
                    request.getPage(),
                    request.getLimit(),
                    pageData.getTotalPages());

            ListResponse<ManagerPaymentSubmissionBatchListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public byte[] export(Integer userId,
            SearchRequest request) {

        List<PaymentSubmissionBatch> batchs = getPaymentSubmissionBatchs(userId, request, null);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PaymentSubmissionBatch");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();

            Font font = workbook.createFont();
            font.setBold(true);

            byte[] fontRgb = new byte[] {
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF
            };
            ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(new XSSFColor(fontRgb, null));

            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            byte[] bgRgb = new byte[] {
                    (byte) 0x1C,
                    (byte) 0x3D,
                    (byte) 0x90
            };
            headerStyle.setFillForegroundColor(new XSSFColor(bgRgb, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] headers = { "Mã phiên đối soát", "Trạng thái",
                    "Tổng tiền hệ thống", "Tổng tiền thực thu",
                    "Tên người nộp", "Số điện thoại người nộp", "Thời gian nộp", "Số đơn hàng",
                    "Tên người xác nhận", "Số điện thoại người xác nhận", "Thời gian xác nhận",
                    "Cập nhật lần cuối", "Ghi chú" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            DecimalFormat df = new DecimalFormat("#,###");
            df.setGroupingUsed(true);
            df.setGroupingSize(3);

            int rowIdx = 1;
            for (PaymentSubmissionBatch sa : batchs) {
                Row row = sheet.createRow(rowIdx++);
                ManagerPaymentSubmissionBatchListDto dto = PaymentSubmissionBatchMapper.toDto(sa);

                row.createCell(0).setCellValue(dto.getCode() != null
                        ? dto.getCode()
                        : "");
                row.createCell(1).setCellValue(dto.getStatus() != null
                        ? PaymentSubmissionBatchUtils.translateStatus(dto.getStatus())
                        : "");
                row.createCell(2).setCellValue(dto.getTotalSystemAmount() != null
                        ? df.format(dto.getTotalSystemAmount()).replace(',', '.')
                        : "");
                row.createCell(3).setCellValue(dto.getTotalActualAmount() != null
                        ? df.format(dto.getTotalActualAmount()).replace(',', '.')
                        : "");
                row.createCell(4)
                        .setCellValue((dto.getShipper() != null && dto.getShipper().getLastName() != null
                                && dto.getShipper().getFirstName() != null)
                                        ? dto.getShipper().getLastName() + " " + dto.getShipper().getFirstName()
                                        : "");
                row.createCell(5)
                        .setCellValue((dto.getShipper() != null && dto.getShipper().getPhoneNumber() != null)
                                ? dto.getShipper().getPhoneNumber()
                                : "");
                row.createCell(6).setCellValue(dto.getCreatedAt() != null
                        ? dtf.format(dto.getCreatedAt())
                        : "");
                row.createCell(7).setCellValue(dto.getTotalOrders() != null
                        ? dto.getTotalOrders()
                        : 0);
                row.createCell(8)
                        .setCellValue((dto.getCheckedBy() != null && dto.getCheckedBy().getLastName() != null
                                && dto.getCheckedBy().getFirstName() != null)
                                        ? dto.getCheckedBy().getLastName() + " " + dto.getCheckedBy().getFirstName()
                                        : "");
                row.createCell(9)
                        .setCellValue((dto.getCheckedBy() != null && dto.getCheckedBy().getPhoneNumber() != null)
                                ? dto.getCheckedBy().getPhoneNumber()
                                : "");
                row.createCell(10).setCellValue(dto.getCheckedAt() != null
                        ? dtf.format(dto.getCheckedAt())
                        : "");
                row.createCell(11).setCellValue((dto.getUpdatedAt() != null && dto.getUpdatedAt() != null)
                        ? dtf.format(dto.getUpdatedAt())
                        : "");
                row.createCell(12).setCellValue(dto.getNotes() != null
                        ? dto.getNotes()
                        : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private Page<PaymentSubmissionBatch> getPaymentSubmissionBatchPage(
            Integer userId,
            SearchRequest request,
            Pageable pageable) {

        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<PaymentSubmissionBatch> spec = PaymentSubmissonBatchSpecification.unrestricted()
                .and(PaymentSubmissonBatchSpecification.officeId(office.getId()))
                .and(PaymentSubmissonBatchSpecification.search(request.getSearch()))
                .and(PaymentSubmissonBatchSpecification.status(request.getStatus()))
                .and(PaymentSubmissonBatchSpecification.createdAtBetween(
                        parseDate(request.getStartDate()),
                        parseDate(request.getEndDate())));

        return batchRepository.findAll(spec, pageable);
    }

    private List<PaymentSubmissionBatch> getPaymentSubmissionBatchs(Integer userId, SearchRequest request,
            Pageable pageable) {
        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<PaymentSubmissionBatch> spec = PaymentSubmissonBatchSpecification.unrestricted()
                .and(PaymentSubmissonBatchSpecification.officeId(office.getId()))
                .and(PaymentSubmissonBatchSpecification.search(request.getSearch()))
                .and(PaymentSubmissonBatchSpecification.status(request.getStatus()))
                .and(PaymentSubmissonBatchSpecification.createdAtBetween(
                        parseDate(request.getStartDate()), parseDate(request.getEndDate())));

        if (pageable != null) {
            return batchRepository.findAll(spec, pageable).getContent();
        } else {
            return batchRepository.findAll(spec, buildSort(request.getSort()));
        }
    }

    private Sort buildSort(String sort) {
        if (sort == null)
            return Sort.unsorted();
        return switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };
    }

    private LocalDateTime parseDate(String s) {
        return (s != null && !s.isBlank()) ? LocalDateTime.parse(s) : null;
    }

    @Transactional
    public void processing(Integer userId, Integer id, ManagerPaymentSubmissionBatchEditForm request) {
        PaymentSubmissionBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_NOT_FOUND));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        User user = userOffice.getManager().getUser();

        if (batch.getOffice() == null || !userOffice.getId().equals(batch.getOffice().getId())) {
            throw new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_ACCESS_DENIED);
        }

        validateForm(request);

        PaymentSubmissionBatchStatus newStatus = PaymentSubmissionBatchStatus.valueOf(request.getStatus());

        if (!PaymentSubmissionBatchUtils.canManagerChangeStatus(batch.getStatus(), newStatus)
                && isBlank(request.getStatus())) {
            throw new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_INVALID_STATUS_CHANGE);
        }

        batch.setStatus(newStatus);
        batch.setNotes(request.getNotes());
        batch.setCheckedAt(LocalDateTime.now());
        batch.setCheckedBy(user);
        batchRepository.save(batch);

        syncSubmissionsWithBatch(batch, newStatus, user);

        if (batch.getShipper() != null && batch.getStatus().equals(PaymentSubmissionBatchStatus.COMPLETED)) {
                notificationService.create(
                        "Phiên đối soát của bạn đã hoàn tất",
                        String.format(
                                "Phiên đối soát #%s của bạn đã được hoàn tất. Tổng tiền COD và phí dịch vụ đã được xác nhận. Vui lòng kiểm tra chi tiết trong hệ thống.",
                                batch.getCode()),
                        "payment_submission",
                        batch.getShipper().getId(),
                        null,
                        "settlements",
                        batch.getId().toString());
        }
    }

    private void syncSubmissionsWithBatch(PaymentSubmissionBatch batch, PaymentSubmissionBatchStatus newStatus,
            User user) {
        List<PaymentSubmission> submissions = batch.getSubmissions();
        if (submissions == null || submissions.isEmpty())
            return;

        LocalDateTime now = LocalDateTime.now();

        for (PaymentSubmission sub : submissions) {
            Order order = sub.getOrder();
            switch (newStatus) {
                case COMPLETED:
                    if (!sub.getStatus().equals(PaymentSubmissionStatus.ADJUSTED)) {
                        sub.setStatus(PaymentSubmissionStatus.MATCHED);
                        sub.setCheckedBy(user);
                        sub.setCheckedAt(now);
                    }


                    if (order != null && order.getCod() > 0) {
                        order.setCodStatus(OrderCodStatus.RECEIVED);
                        orderRepository.save(order);
                    }
                    break;
                case PROCESSING:
                    sub.setStatus(PaymentSubmissionStatus.PROCESSING);

                    if (order != null && order.getCod() > 0) {
                        order.setCodStatus(OrderCodStatus.SUBMITTED);
                        orderRepository.save(order);
                    }
                    break;
                case OPEN:
                    break;
            }
        }

        submissionRepository.saveAll(submissions);
    }

    private void validateForm(ManagerPaymentSubmissionBatchEditForm request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus()))
            missing.add("Trạng thái");

        if (!missing.isEmpty())
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missing));

        PaymentSubmissionBatchStatus status;
        try {
            status = PaymentSubmissionBatchStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_INVALID_STATUS);
        }

        if (!isBlank(request.getNotes()) && request.getNotes().length() > 255) {
            throw new AppException(PaymentSubmissionBatchErrorCode.PAYMENT_SUBMISSION_BATCH_INVALID_NOTE);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}