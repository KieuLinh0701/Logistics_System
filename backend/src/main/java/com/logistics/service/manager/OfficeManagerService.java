package com.logistics.service.manager;

import com.logistics.dto.OfficeDto;
import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;
import com.logistics.mapper.OfficeMapper;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.manager.ManagerOfficeEditRequest;
import com.logistics.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeManagerService {

    private final OfficeRepository officeRepository;

    private final EmployeeManagerService employeeManagerService;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ApiResponse<OfficeDto> getMyOffice(int userId) {
        try {
            Office office = employeeManagerService.getOfficeByUserId(userId);
            OfficeDto dto = OfficeMapper.toDto(office);
            return new ApiResponse<>(true, "Lấy thông tin bưu cục thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> updateMyOffice(int userId, ManagerOfficeEditRequest request) {
        try {

            String validationError = validateRequest(request);
            if (validationError != null) {
                return new ApiResponse<>(false, validationError, false);
            }

            Office office = employeeManagerService.getOfficeByUserId(userId);
            if (office == null) {
                return new ApiResponse<>(false, "Không tìm thấy bưu cục của manager", false);
            }

            office.setEmail(request.getEmail());
            if (request.getStatus() != null) {
                office.setStatus(OfficeStatus.valueOf(request.getStatus()));
            }
            office.setCapacity(request.getCapacity());
            office.setNotes(request.getNotes());

            if (request.getOpeningTime() != null) {
                office.setOpeningTime(LocalTime.parse(request.getOpeningTime(), TIME_FORMAT));
            }
            if (request.getClosingTime() != null) {
                office.setClosingTime(LocalTime.parse(request.getClosingTime(), TIME_FORMAT));
            }

            officeRepository.save(office);

            return new ApiResponse<>(true, "Cập nhật bưu cục thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    private String validateRequest(ManagerOfficeEditRequest request) {
        if (request.getEmail() == null ||
                !request.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Email không hợp lệ";
        }

        if (request.getStatus() != null) {
            try {
                OfficeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                return "Status không hợp lệ";
            }
        }

        if (request.getCapacity() != null && request.getCapacity() < 0) {
            return "Capacity phải lớn hơn hoặc bằng 0";
        }

        if (request.getOpeningTime() != null) {
            try {
                LocalTime.parse(request.getOpeningTime(), TIME_FORMAT);
            } catch (DateTimeParseException e) {
                return "OpeningTime phải đúng định dạng HH:mm";
            }
        }

        if (request.getClosingTime() != null) {
            try {
                LocalTime.parse(request.getClosingTime(), TIME_FORMAT);
            } catch (DateTimeParseException e) {
                return "ClosingTime phải đúng định dạng HH:mm";
            }
        }

        return null;
    }
}