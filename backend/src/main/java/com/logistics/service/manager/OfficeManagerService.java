package com.logistics.service.manager;

import com.logistics.dto.OfficeDto;
import com.logistics.entity.Office;
import com.logistics.enums.OfficeStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.OfficeErrorCode;
import com.logistics.mapper.OfficeMapper;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.manager.ManagerOfficeEditRequest;
import com.logistics.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeManagerService {

    private final OfficeRepository officeRepository;

    private final EmployeeManagerService employeeManagerService;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public OfficeDto getMyOffice(int userId) {
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            return OfficeMapper.toDto(office);
    }

    public void updateMyOffice(int userId, ManagerOfficeEditRequest request) {
            validateRequest(request);

            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            if (office == null) {
                throw new AppException(OfficeErrorCode.OFFICE_NOT_FOUND);
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
    }

    private void validateRequest(ManagerOfficeEditRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getEmail() == null ||
                !request.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("Email không hợp lệ");
        }

        if (request.getStatus() != null) {
            try {
                OfficeStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                errors.add("Trạng thái không hợp lệ");
            }
        }

        if (request.getCapacity() != null && request.getCapacity() < 0) {
            errors.add("Dung tich chưa phải lớn hơn hoặc bằng 0");
        }

        validateTimeFormat(request.getOpeningTime(), "Thời gian mở cửa", errors);
        validateTimeFormat(request.getClosingTime(), "Thời gian đóng cửa", errors);

        if (!errors.isEmpty()) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", errors));
        }
    }

    private void validateTimeFormat(String time, String fieldName, List<String> errors) {
        if (time != null) {
            try {
                LocalTime.parse(time, TIME_FORMAT);
            } catch (DateTimeParseException e) {
                errors.add(fieldName + " phải đúng định dạng HH:mm");
            }
        }
    }

    public Integer getMyOfficeCityCode(int userId) {
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            return office.getCityCode();
    }
}