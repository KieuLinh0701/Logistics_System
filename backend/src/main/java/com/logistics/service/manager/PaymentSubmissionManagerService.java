package com.logistics.service.manager;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.manager.paymentSubmission.ManagerPaymentSubmissionListDto;
import com.logistics.entity.Office;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.entity.User;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.mapper.PaymentSubmissionMapper;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.ManagerPaymentSubmissionForm;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.PaymentSubmissonSpecification;
import com.logistics.utils.PaymentSubmissionUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentSubmissionManagerService {

    private final EmployeeManagerService employeeManagerService;

    private final PaymentSubmissionRepository paymentSubmissionRepository;

    private final PaymentSubmissionBatchRepository batchRepository;

    public ApiResponse<ListResponse<ManagerPaymentSubmissionListDto>> list(
            Integer userId, Integer batchId, SearchRequest request) {
        try {
            Sort sort = buildSort(request.getSort());
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
            Page<PaymentSubmission> pageData = getPaymentSubmissionPage(userId, batchId, request, pageable);

            List<ManagerPaymentSubmissionListDto> list = pageData.getContent()
                    .stream()
                    .map(PaymentSubmissionMapper::toDto)
                    .toList();

            Pagination pagination = new Pagination(
                    (int) pageData.getTotalElements(),
                    request.getPage(),
                    request.getLimit(),
                    pageData.getTotalPages());

            ListResponse<ManagerPaymentSubmissionListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách đối soát thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public byte[] export(Integer userId, Integer batchId, SearchRequest request) {

        List<PaymentSubmission> submissions = getPaymentSubmissions(userId, batchId, request, null);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PaymentSubmission");

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
            String[] headers = { "Mã đối soát", "Trạng thái",
                    "Ngày nộp",
                    "Số tiền hệ thống", "Số tiền thực thu", "Mã đơn",
                    "Tên người xác nhận", "Số điện thoại người xác nhận",
                    "Thời gian xác nhận", "Cập nhật lần cuối", "Ghi chú" };
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
            for (PaymentSubmission sa : submissions) {
                Row row = sheet.createRow(rowIdx++);
                ManagerPaymentSubmissionListDto dto = PaymentSubmissionMapper.toDto(sa);

                row.createCell(0).setCellValue(dto.getCode() != null
                        ? dto.getCode()
                        : "");
                row.createCell(1).setCellValue(dto.getStatus() != null
                        ? PaymentSubmissionUtils.translateStatus(dto.getStatus())
                        : "");
                row.createCell(2).setCellValue(dto.getPaidAt() != null
                        ? dtf.format(dto.getPaidAt())
                        : "");
                row.createCell(3).setCellValue(dto.getSystemAmount() != null
                        ? df.format(dto.getSystemAmount()).replace(',', '.')
                        : "");
                row.createCell(4).setCellValue(dto.getActualAmount() != null
                        ? df.format(dto.getActualAmount()).replace(',', '.')
                        : "");
                row.createCell(5).setCellValue((dto.getOrder() != null && dto.getOrder().getTrackingNumber() != null)
                        ? dto.getOrder().getTrackingNumber()
                        : "");
                row.createCell(6)
                        .setCellValue((dto.getCheckedBy() != null && dto.getCheckedBy().getLastName() != null
                                && dto.getCheckedBy().getFirstName() != null)
                                        ? dto.getCheckedBy().getLastName() + " " + dto.getCheckedBy().getFirstName()
                                        : "");
                row.createCell(7)
                        .setCellValue((dto.getCheckedBy() != null && dto.getCheckedBy().getPhoneNumber() != null)
                                ? dto.getCheckedBy().getPhoneNumber()
                                : "");
                row.createCell(8).setCellValue(dto.getCheckedAt() != null
                        ? dtf.format(dto.getCheckedAt())
                        : "");
                row.createCell(9).setCellValue((dto.getPaidAt() != null && dto.getCheckedAt() != null)
                        ? dtf.format(dto.getUpdatedAt())
                        : "");
                row.createCell(10).setCellValue(dto.getNotes() != null
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }

    private Page<PaymentSubmission> getPaymentSubmissionPage(
            Integer userId,
            Integer batchId,
            SearchRequest request,
            Pageable pageable) {

        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        PaymentSubmissionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Phiên đối soát không tồn tại"));

        if (!batch.getOffice().getId().equals(office.getId())) {
            throw new RuntimeException("Bạn không có quyền xem batch này");
        }

        Specification<PaymentSubmission> spec = PaymentSubmissonSpecification.unrestricted()
                .and(PaymentSubmissonSpecification.batchId(batch.getId()))
                .and(PaymentSubmissonSpecification.search(request.getSearch()))
                .and(PaymentSubmissonSpecification.status(request.getStatus()))
                .and(PaymentSubmissonSpecification.createdAtBetween(
                        parseDate(request.getStartDate()),
                        parseDate(request.getEndDate())));

        return paymentSubmissionRepository.findAll(spec, pageable);
    }

    private List<PaymentSubmission> getPaymentSubmissions(Integer userId, @NonNull Integer batchId,
            SearchRequest request,
            Pageable pageable) {
        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        PaymentSubmissionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Phiên đối soát không tồn tại"));

        if (!batch.getOffice().getId().equals(office.getId())) {
            throw new RuntimeException("Bạn không có quyền xem batch này");
        }

        Specification<PaymentSubmission> spec = PaymentSubmissonSpecification.unrestricted()
                .and(PaymentSubmissonSpecification.batchId(batch.getId()))
                .and(PaymentSubmissonSpecification.search(request.getSearch()))
                .and(PaymentSubmissonSpecification.status(request.getStatus()))
                .and(PaymentSubmissonSpecification.createdAtBetween(
                        parseDate(request.getStartDate()), parseDate(request.getEndDate())));

        if (pageable != null) {
            return paymentSubmissionRepository.findAll(spec, pageable).getContent();
        } else {
            return paymentSubmissionRepository.findAll(spec, buildSort(request.getSort()));
        }
    }

    private Sort buildSort(String sort) {
        if (sort == null)
            return Sort.unsorted();
        return switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("paidAt").descending();
            case "oldest" -> Sort.by("paidAt").ascending();
            default -> Sort.unsorted();
        };
    }

    private LocalDateTime parseDate(String s) {
        return (s != null && !s.isBlank()) ? LocalDateTime.parse(s) : null;
    }

    public ApiResponse<Boolean> processing(int userId, int id, ManagerPaymentSubmissionForm request) {

        PaymentSubmission submission = paymentSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đối soát không tồn tại"));

        if (!checkPermission(userId, submission)) {
            return new ApiResponse<>(false, "Không có quyền xử lý đối soát này", null);
        }

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        User user = userOffice.getManager().getUser();

        validateForm(request);

        PaymentSubmissionStatus newStatus = PaymentSubmissionStatus.valueOf(request.getStatus());

        if (!PaymentSubmissionUtils.canManagerChangeStatus(submission.getStatus(), newStatus)
                && isBlank(request.getStatus())) {
            throw new RuntimeException("Trạng thái yêu cầu chuyển không hợp lệ");
        }

        submission.setStatus(newStatus);
        submission.setNotes(request.getNotes());
        submission.setCheckedAt(LocalDateTime.now());
        submission.setCheckedBy(user);
        paymentSubmissionRepository.save(submission);

        return new ApiResponse<>(true, "Đối soát thành công", true);
    }

    private boolean checkPermission(int userId, PaymentSubmission submission) {
        if (submission == null) {
            return false;
        }

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (userOffice == null) {
            return false;
        }

        if (submission.getOrder() == null || submission.getOrder().getToOffice() == null
                || submission.getOrder().getToOffice().getId() == null) {
            return false;
        }

        return submission.getOrder().getToOffice().getId().equals(userOffice.getId());
    }

    private void validateForm(ManagerPaymentSubmissionForm request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus()))
            missing.add("Trạng thái");

        if (!missing.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));

        PaymentSubmissionStatus status;
        try {
            status = PaymentSubmissionStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Trạng thái yêu cầu không hợp lệ");
        }

        if (!isBlank(request.getNotes()) && request.getNotes().length() > 255) {
            throw new RuntimeException("Ghi chú đối soát không được vượt quá 255 ký tự");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}