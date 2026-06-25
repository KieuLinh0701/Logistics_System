package com.logistics.service.shipper;

import com.logistics.dto.shipper.shipment.ShipperShipmentDetailDto;
import com.logistics.dto.shipper.shipment.ShipperShipmentListDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Shipment;
import com.logistics.entity.ShipmentOrder;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.OrderStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.ShipmentErrorCode;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.*;
import com.logistics.request.shipper.ShipperOrdersShipmentSearchRequest;
import com.logistics.request.shipper.ShipperShipmentSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.service.manager.EmployeeManagerService;
import com.logistics.specification.ShipmentSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.logistics.utils.OrderUtils.*;
import static com.logistics.utils.ShipmentUtils.translateShipmentStatus;

@Service
@RequiredArgsConstructor
public class ShipmentHistoryShipperService {

    private final ShipmentRepository repository;
    private final ShipmentRepository shipmentRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final OfficeRepository officeRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepository;
    private final EmployeeManagerService employeeManagerService;
    private final NotificationService notificationService;

    private Employee getCurrentShipperEmployee(Integer userId) {
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.stream()
                .filter(e -> e.getAccountRole() != null
                        && e.getAccountRole().getRole() != null
                        && "Shipper".equals(e.getAccountRole().getRole().getName())
                        && e.getAccountRole().getRole().getUserOwner() == null
                        && EmployeeStatus.ACTIVE.equals(e.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    public ListResponse<ShipperShipmentListDto> list(
            int userId,
            ShipperShipmentSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Employee employee = getCurrentShipperEmployee(userId);

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.employeeId(employee.getId()))
                .and(ShipmentSpecification.search(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Shipment> pageData = repository.findAll(spec, pageable);

        List<ShipperShipmentListDto> list = pageData.getContent()
                .stream()
                .map(ShipmentMapper::toShipperShipmentListDto)
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ShipperShipmentListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public ListResponse<ShipperShipmentDetailDto> getOrdersByShipmentId(
            int userId,
            int shipmentId,
            ShipperOrdersShipmentSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();

        Employee employee = getCurrentShipperEmployee(userId);

        Shipment shipment = repository.findByEmployeeIdAndId(employee.getId(), shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        List<ShipperShipmentDetailDto> orders = shipment.getShipmentOrders()
                .stream()
                .map(ShipmentOrder::getOrder)
                .filter(o -> search == null || o.getTrackingNumber().contains(search))
                .map(OrderMapper::toShipperShipmentDetailDto)
                .toList();

        int total = orders.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        List<ShipperShipmentDetailDto> pagedList = orders.subList(fromIndex, toIndex);

        Pagination pagination = new Pagination(total, page, limit, (int) Math.ceil((double) total / limit));

        ListResponse<ShipperShipmentDetailDto> data = new ListResponse<>();
        data.setList(pagedList);
        data.setPagination(pagination);

        return data;
    }

    public byte[] export(int userId, ShipperShipmentSearchRequest request) {
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Employee employee = getCurrentShipperEmployee(userId);

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.employeeId(employee.getId()))
                .and(ShipmentSpecification.search(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Shipment> shipments = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shipments");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã chuyến",
                    "Trạng thái",
                    "Nhân viên tạo chuyến",
                    "Mã NV tạo",
                    "SĐT NV tạo",
                    "Email NV tạo",
                    "Thời gian bắt đầu",
                    "Thời gian kết thúc",
                    "Thời gian tạo",
                    "Thời gian cập nhật"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (Shipment s : shipments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(s.getCode() != null ? s.getCode() : "");
                row.createCell(1).setCellValue(translateShipmentStatus(s.getStatus()));

                if (s.getCreatedBy() != null) {
                    row.createCell(3).setCellValue(s.getCreatedBy().getCode() != null ? s.getCreatedBy().getCode() : "");

                    if (s.getCreatedBy().getUser() != null) {
                        String lastName = s.getCreatedBy().getUser().getLastName() != null ? s.getCreatedBy().getUser().getLastName() : "";
                        String firstName = s.getCreatedBy().getUser().getFirstName() != null ? s.getCreatedBy().getUser().getFirstName() : "";
                        row.createCell(2).setCellValue((lastName + " " + firstName).trim());

                        row.createCell(4).setCellValue(s.getCreatedBy().getUser().getPhoneNumber() != null ? s.getCreatedBy().getUser().getPhoneNumber() : "");

                        if (s.getCreatedBy().getUser().getAccount() != null) {
                            row.createCell(5).setCellValue(s.getCreatedBy().getUser().getAccount().getEmail() != null ? s.getCreatedBy().getUser().getAccount().getEmail() : "");
                        } else {
                            row.createCell(5).setCellValue("");
                        }
                    } else {
                        row.createCell(2).setCellValue("N/A");
                        row.createCell(4).setCellValue("");
                        row.createCell(5).setCellValue("");
                    }
                } else {
                    row.createCell(2).setCellValue("N/A");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue("");
                }

                row.createCell(6).setCellValue(s.getStartTime() != null ? s.getStartTime().format(dtf) : "N/A");
                row.createCell(7).setCellValue(s.getEndTime() != null ? s.getEndTime().format(dtf) : "N/A");
                row.createCell(8).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().format(dtf) : "N/A");
                row.createCell(9).setCellValue(s.getUpdatedAt() != null ? s.getUpdatedAt().format(dtf) : "N/A");
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

    public byte[] exportOrdersByShipmentId(
            int userId,
            int shipmentId,
            ShipperOrdersShipmentSearchRequest request) {
        String search = request.getSearch();

        Employee employee = getCurrentShipperEmployee(userId);

        Shipment shipment = repository.findByEmployeeIdAndId(employee.getId(), shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        List<ShipperShipmentDetailDto> orders = shipment.getShipmentOrders()
                .stream()
                .map(ShipmentOrder::getOrder)
                .filter(o -> search == null || o.getTrackingNumber().contains(search))
                .map(OrderMapper::toShipperShipmentDetailDto)
                .toList();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders_" + shipment.getCode());

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã đơn",
                    "Trạng thái",
                    "Người thanh toán",
                    "Trạng thái thanh toán",
                    "Phí thu hộ (VNĐ)",
                    "Trọng lượng (Kg)",
                    "Tên người gửi",
                    "SĐT người gửi",
                    "Địa chỉ người gửi",
                    "Tên người nhận",
                    "SĐT người nhận",
                    "Địa chỉ người nhận"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ShipperShipmentDetailDto o : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(o.getTrackingNumber() != null ? o.getTrackingNumber() : "Chưa có mã");
                row.createCell(1).setCellValue(translateOrderStatus(OrderStatus.valueOf(o.getStatus())));
                row.createCell(2).setCellValue(translateOrderPayerType(OrderPayerType.valueOf(o.getPayer())));
                row.createCell(3).setCellValue(translateOrderPaymentStatus(OrderPaymentStatus.valueOf(o.getPaymentStatus())));

                double cod = o.getCod() != null ? o.getCod().doubleValue() : 0;
                double totalFee = o.getTotalFee() != null ? o.getTotalFee().doubleValue() : 0;
                double fee = "CUSTOMER".equals(o.getPayer()) ? cod + totalFee : cod;
                row.createCell(4).setCellValue(fee);

                row.createCell(5).setCellValue(o.getWeight() != null ? o.getWeight().doubleValue() : 0);

                row.createCell(6).setCellValue(o.getSenderName() != null ? o.getSenderName() : "");
                row.createCell(7).setCellValue(o.getSenderPhone() != null ? o.getSenderPhone() : "");
                row.createCell(8).setCellValue(o.getSenderFullAddress() != null ? o.getSenderFullAddress() : "Chưa có địa chỉ");

                row.createCell(9).setCellValue(o.getRecipientName() != null ? o.getRecipientName() : "");
                row.createCell(10).setCellValue(o.getRecipientPhone() != null ? o.getRecipientPhone() : "");
                row.createCell(11).setCellValue(o.getRecipientFullAddress() != null ? o.getRecipientFullAddress() : "Chưa có địa chỉ");
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
}