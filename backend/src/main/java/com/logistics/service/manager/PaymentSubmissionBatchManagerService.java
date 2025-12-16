package com.logistics.service.manager;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.entity.User;
import com.logistics.enums.PaymentSubmissionBatchStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.mapper.PaymentSubmissionBatchMapper;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchCreateForm;
import com.logistics.request.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchEditForm;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.PaymentSubmissonBatchSpecification;
import com.logistics.utils.PaymentSubmissionBatchUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentSubmissionBatchManagerService {

    private final EmployeeManagerService employeeManagerService;

    private final EmployeeRepository employeeRepository;

    private final PaymentSubmissionBatchRepository batchRepository;

    private final PaymentSubmissionRepository submissionRepository;

    private final NotificationService notificationService;

    public ApiResponse<ListResponse<ManagerPaymentSubmissionBatchListDto>> list(
            Integer userId, SearchRequest request) {
        try {
            Sort sort = buildSort(request.getSort());
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
            List<PaymentSubmissionBatch> submissions = getPaymentSubmissionBatchs(userId, request, pageable);

            List<ManagerPaymentSubmissionBatchListDto> list = submissions.stream()
                    .map(PaymentSubmissionBatchMapper::toDto)
                    .toList();

            int total = submissions.size();
            Pagination pagination = new Pagination(total, request.getPage(), request.getLimit(), 1);

            ListResponse<ManagerPaymentSubmissionBatchListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách phiên đối soát thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
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
    public ApiResponse<Boolean> create(Integer userId, ManagerPaymentSubmissionBatchCreateForm form) {
        try {
            Office managedOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Employee employeeShipper = employeeRepository.findById(form.getShipperId())
                    .orElseThrow(() -> new RuntimeException("Nhân viên giao hàng không tồn tại"));

            if (employeeShipper.getOffice() == null
                    || !employeeShipper.getOffice().getId().equals(managedOffice.getId())) {
                throw new RuntimeException("Nhân viên này không thuộc bưu cục của bạn");
            }

            User shipper = employeeShipper.getUser();

            PaymentSubmissionBatch batch = new PaymentSubmissionBatch();
            batch.setShipper(shipper);
            batch.setTotalActualAmount(form.getTotalActualAmount());
            batch.setTotalSystemAmount(BigDecimal.ZERO);
            batch.setStatus(PaymentSubmissionBatchStatus.PENDING);
            batch.setOffice(managedOffice);

            batchRepository.save(batch);

            List<PaymentSubmission> submissions = submissionRepository
                    .findByBatchIsNullAndStatusIn(List.of(PaymentSubmissionStatus.PENDING));

            for (PaymentSubmission ps : submissions) {
                ps.setBatch(batch);
                ps.setStatus(PaymentSubmissionStatus.IN_BATCH);
            }
            submissionRepository.saveAll(submissions);

            BigDecimal totalSystem = submissions.stream()
                    .map(PaymentSubmission::getSystemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            batch.setTotalSystemAmount(totalSystem);
            batchRepository.save(batch);

            return new ApiResponse<>(true, "Tạo phiên đối soát thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> processing(Integer userId, Integer id, ManagerPaymentSubmissionBatchEditForm request) {
        PaymentSubmissionBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Phiên đối soát không tồn tại"));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (userOffice == null
                || userOffice.getManager() == null
                || !userOffice.getManager().getId().equals(userId)) {
            return new ApiResponse<>(false, "Không có quyền xử lý phiên đối soát này", null);
        }

        User user = userOffice.getManager().getUser();

        if (batch.getOffice() == null || !userOffice.getId().equals(batch.getOffice().getId())) {
            return new ApiResponse<>(false, "Không có quyền xử lý phiên đối soát này", null);
        }

        validateForm(request);

        PaymentSubmissionBatchStatus newStatus = PaymentSubmissionBatchStatus.valueOf(request.getStatus());

        if (!PaymentSubmissionBatchUtils.canManagerChangeStatus(batch.getStatus(), newStatus)
                && isBlank(request.getStatus())) {
            throw new RuntimeException("Trạng thái yêu cầu chuyển không hợp lệ");
        }

        batch.setStatus(newStatus);
        batch.setNotes(request.getNotes());
        batch.setCheckedAt(LocalDateTime.now());
        batch.setCheckedBy(user);
        batchRepository.save(batch);

        syncSubmissionsWithBatch(batch, newStatus, user);

        if (batch.getShipper() != null) {
            if (batch.getStatus().equals(PaymentSubmissionBatchStatus.COMPLETED)) {
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
            } else if (batch.getStatus().equals(PaymentSubmissionBatchStatus.PARTIAL)) {
                notificationService.create(
                        "Phiên đối soát của bạn đang được xử lý",
                        String.format(
                                "Phiên đối soát #%s của bạn đang được xử lý một phần. Một số đơn hàng đã được soát, một số đang chờ cập nhật. Vui lòng kiểm tra chi tiết trong hệ thống.",
                                batch.getCode()),
                        "payment_submission",
                        batch.getShipper().getId(),
                        null,
                        "settlements",
                        batch.getId().toString());
            }
        }

        return new ApiResponse<>(true, "Cập nhật phiên đối soát thành công", true);
    }

    private void syncSubmissionsWithBatch(PaymentSubmissionBatch batch, PaymentSubmissionBatchStatus newStatus,
            User user) {
        List<PaymentSubmission> submissions = batch.getSubmissions();
        if (submissions == null || submissions.isEmpty())
            return;

        LocalDateTime now = LocalDateTime.now();

        for (PaymentSubmission sub : submissions) {
            switch (newStatus) {
                case PARTIAL:
                    sub.setStatus(PaymentSubmissionStatus.MISMATCHED);
                    sub.setCheckedBy(user);
                    sub.setCheckedAt(now);
                    break;
                case COMPLETED:
                    if (!sub.getStatus().equals(PaymentSubmissionStatus.ADJUSTED)) {
                        sub.setStatus(PaymentSubmissionStatus.MATCHED);
                        sub.setCheckedBy(user);
                        sub.setCheckedAt(now);
                    }
                    break;
                case CANCELLED:
                    sub.setStatus(PaymentSubmissionStatus.PENDING);
                    sub.setBatch(null);
                    break;
                case CHECKING:
                    break;
                case PENDING:
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
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));

        PaymentSubmissionBatchStatus status;
        try {
            status = PaymentSubmissionBatchStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Trạng thái yêu cầu không hợp lệ");
        }

        if (!isBlank(request.getNotes()) && request.getNotes().length() > 255) {
            throw new RuntimeException("Ghi chú phiên đối soát không được vượt quá 255 ký tự");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}