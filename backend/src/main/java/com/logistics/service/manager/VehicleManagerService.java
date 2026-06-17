package com.logistics.service.manager;

import com.logistics.dto.VehicleDto;
import com.logistics.entity.Office;
import com.logistics.entity.Vehicle;
import com.logistics.enums.VehicleStatus;
import com.logistics.mapper.VehicleMapper;
import com.logistics.repository.VehicleRepository;
import com.logistics.request.manager.vehicle.ManagerVehicleEditRequest;
import com.logistics.request.manager.vehicle.ManagerVehicleSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.VehicleSpecification;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.logistics.utils.VehicleUtils.translateVehicleStatus;
import static com.logistics.utils.VehicleUtils.translateVehicleType;

@Service
@RequiredArgsConstructor
public class VehicleManagerService {

    private final VehicleRepository vehicleRepository;

    private final EmployeeManagerService employeeManagerService;

    public ApiResponse<ListResponse<VehicleDto>> list(int userId, ManagerVehicleSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String type = request.getType();
            String status = request.getStatus();
            String sort = request.getSort();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            Specification<Vehicle> spec = VehicleSpecification.unrestrictedVehicle()
                    .and(VehicleSpecification.officeId(office.getId()))
                    .and(VehicleSpecification.search(search))
                    .and(VehicleSpecification.type(type))
                    .and(VehicleSpecification.status(status))
                    .and(VehicleSpecification.createdAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("createdAt").descending();
                case "oldest" -> Sort.by("createdAt").ascending();
                case "capacity_high" -> Sort.by("capacity").descending();
                case "capacity_low" -> Sort.by("capacity").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Vehicle> pageData = vehicleRepository.findAll(spec, pageable);

            List<VehicleDto> list = pageData.getContent()
                    .stream()
                    .map(VehicleMapper::toDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<VehicleDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách phương tiện thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public byte[] export(int userId, ManagerVehicleSearchRequest request) {
        String search = request.getSearch();
        String type = request.getType();
        String status = request.getStatus();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<Vehicle> spec = VehicleSpecification.unrestrictedVehicle()
                .and(VehicleSpecification.officeId(office.getId()))
                .and(VehicleSpecification.search(search))
                .and(VehicleSpecification.type(type))
                .and(VehicleSpecification.status(status))
                .and(VehicleSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "capacity_high" -> Sort.by("capacity").descending();
            case "capacity_low" -> Sort.by("capacity").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Vehicle> vehicles = vehicleRepository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vehicles");

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
                    "Biển số xe",
                    "Loại xe",
                    "Tải trọng (Kg)",
                    "Trạng thái",
                    "Mô tả",
                    "Bảo trì gần nhất",
                    "Bảo trì tiếp theo",
                    "Vĩ độ", "Kinh độ",
                    "Mã thiết bị GPS"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

            int rowIdx = 1;
            for (Vehicle v : vehicles) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(v.getLicensePlate() != null ? v.getLicensePlate() : "");
                row.createCell(1).setCellValue(translateVehicleType(v.getType()));
                row.createCell(2).setCellValue(v.getCapacity() != null ? v.getCapacity().doubleValue() : 0);
                row.createCell(3).setCellValue(translateVehicleStatus(v.getStatus()));
                row.createCell(4).setCellValue(v.getDescription() != null ? v.getDescription() : "N/A");
                row.createCell(5).setCellValue(v.getLastMaintenanceAt() != null ? v.getLastMaintenanceAt().format(dtf) : "N/A");
                row.createCell(6).setCellValue(v.getNextMaintenanceDue() != null ? v.getNextMaintenanceDue().format(dtf) : "N/A");
                row.createCell(7).setCellValue(v.getLatitude() != null ? v.getLatitude().doubleValue() : 0);
                row.createCell(8).setCellValue(v.getLongitude() != null ? v.getLongitude().doubleValue() : 0);
                row.createCell(9).setCellValue(v.getGpsDeviceId() != null ? v.getGpsDeviceId() : "");
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

    public ApiResponse<Boolean> update(int userId, int vehicleId, ManagerVehicleEditRequest request) {
        try {

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            if (!userOffice.getId().equals(vehicle.getOffice().getId())) {
                throw new RuntimeException("Người dùng không có quyền chỉnh sửa xe tại bưu cục này");
            }

            if (request.getDescription() != null) {
                vehicle.setDescription(request.getDescription().trim());
            }

            if (request.getStatus() != null) {
                try {
                    VehicleStatus newStatus = VehicleStatus.valueOf(request.getStatus());

                    if (!newStatus.equals(vehicle.getStatus()) && newStatus == VehicleStatus.MAINTENANCE) {
                        vehicle.setLastMaintenanceAt(LocalDateTime.now());
                    }

                    vehicle.setStatus(newStatus);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Trạng thái không hợp lệ");
                }
            }

            if (request.getNextMaintenanceDue() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (request.getNextMaintenanceDue().isBefore(now)) {
                    throw new RuntimeException("Ngày bảo trì tiếp theo phải lớn hơn hoặc bằng hiện tại");
                }
                vehicle.setNextMaintenanceDue(request.getNextMaintenanceDue());
            }

            if (request.getGpsDeviceId() != null) {
                vehicle.setGpsDeviceId(request.getGpsDeviceId().trim());
            }

            vehicleRepository.save(vehicle);

            return new ApiResponse<>(true, "Cập nhật xe thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<List<VehicleDto>> getAvailableVehicles(int userId) {
        try {
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            Specification<Vehicle> spec = VehicleSpecification.unrestrictedVehicle()
                    .and(VehicleSpecification.officeId(office.getId()))
                    .and(VehicleSpecification.status("AVAILABLE"));

            List<Vehicle> vehicles = vehicleRepository.findAll(spec);

            List<VehicleDto> list = vehicles.stream()
                    .map(VehicleMapper::toDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách phương tiện thành công", list);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }
}