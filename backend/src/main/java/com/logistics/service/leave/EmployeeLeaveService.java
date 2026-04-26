package com.logistics.service.leave;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.leave.EmployeeLeaveDto;
import com.logistics.entity.Employee;
import com.logistics.entity.EmployeeLeaveRequest;
import com.logistics.entity.Office;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.LeaveReasonType;
import com.logistics.enums.LeaveRequestStatus;
import com.logistics.exception.LeaveException;
import com.logistics.repository.EmployeeLeaveRequestRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.request.leave.ApproveLeaveRequest;
import com.logistics.request.leave.CreateLeaveRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.manager.EmployeeManagerService;
import com.logistics.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeLeaveService {

    private final EmployeeLeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeManagerService employeeManagerService;

    @Transactional
    public ApiResponse<EmployeeLeaveDto> createLeave(CreateLeaveRequest request) {
        try {
            Employee employee = getCurrentEmployeeForDriverOrShipper();
            validateLeaveInput(request);

            Office office = employee.getOffice();
            if (office == null) {
                throw new LeaveException("Nhân viên chưa được gán bưu cục");
            }

            EmployeeLeaveRequest leave = new EmployeeLeaveRequest();
            leave.setEmployee(employee);
            leave.setOffice(office);
            leave.setLeaveDate(request.getLeaveDate());
            leave.setShift(request.getShift());
            leave.setReasonType(request.getReasonType());
            leave.setCustomReason(cleanText(request.getCustomReason()));
            leave.setEmployeeNote(cleanText(request.getEmployeeNote()));
            leave.setStatus(LeaveRequestStatus.PENDING);

            EmployeeLeaveRequest saved = leaveRepository.save(leave);
            return new ApiResponse<>(true, "Gửi đơn xin nghỉ thành công", toDto(saved));
        } catch (LeaveException e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Gửi đơn xin nghỉ thất bại: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<EmployeeLeaveDto>> getMyLeaves() {
        try {
            Employee employee = getCurrentEmployeeForDriverOrShipper();

            List<EmployeeLeaveDto> data = leaveRepository.findByEmployeeId(employee.getId())
                    .stream()
                    .sorted(Comparator.comparing(EmployeeLeaveRequest::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::toDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách đơn nghỉ phép thành công", data);
        } catch (LeaveException e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lấy danh sách đơn nghỉ phép thất bại: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> cancelLeave(Integer leaveId) {
        try {
            Employee employee = getCurrentEmployeeForDriverOrShipper();

            EmployeeLeaveRequest leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new LeaveException("Không tìm thấy đơn xin nghỉ"));

            if (!leave.getEmployee().getId().equals(employee.getId())) {
                throw new LeaveException("Bạn không có quyền hủy đơn nghỉ phép này");
            }

            if (leave.getStatus() != LeaveRequestStatus.PENDING) {
                throw new LeaveException("Chỉ có thể hủy đơn ở trạng thái chờ duyệt");
            }

            leave.setStatus(LeaveRequestStatus.CANCELLED);
            leaveRepository.save(leave);

            return new ApiResponse<>(true, "Hủy đơn nghỉ phép thành công", true);
        } catch (LeaveException e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Hủy đơn nghỉ phép thất bại: " + e.getMessage(), false);
        }
    }

    public ApiResponse<List<EmployeeLeaveDto>> getOfficeLeaves() {
        try {
            Office office = getManagedOffice();

            List<EmployeeLeaveDto> data = leaveRepository.findByOfficeId(office.getId())
                    .stream()
                    .sorted(Comparator.comparing(EmployeeLeaveRequest::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::toDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách đơn nghỉ phép của bưu cục thành công", data);
        } catch (LeaveException e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lấy danh sách đơn nghỉ phép thất bại: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<EmployeeLeaveDto> approveLeave(Integer leaveId, ApproveLeaveRequest request) {
        try {
            Office office = getManagedOffice();
            Employee managerEmployee = getCurrentManagerEmployeeInOffice(office.getId());

            EmployeeLeaveRequest leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new LeaveException("Không tìm thấy đơn xin nghỉ"));

            if (leave.getOffice() == null || !leave.getOffice().getId().equals(office.getId())) {
                throw new LeaveException("Bạn không có quyền duyệt đơn nghỉ phép của bưu cục khác");
            }

            if (leave.getStatus() != LeaveRequestStatus.PENDING) {
                throw new LeaveException("Chỉ có thể duyệt/từ chối đơn ở trạng thái chờ duyệt");
            }

            if (request.getStatus() != LeaveRequestStatus.APPROVED
                    && request.getStatus() != LeaveRequestStatus.REJECTED) {
                throw new LeaveException("Trạng thái duyệt chỉ được là APPROVED hoặc REJECTED");
            }

            leave.setStatus(request.getStatus());
            leave.setApprovedBy(managerEmployee);

            EmployeeLeaveRequest saved = leaveRepository.save(leave);
            return new ApiResponse<>(true, "Cập nhật trạng thái đơn nghỉ phép thành công", toDto(saved));
        } catch (LeaveException e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Duyệt đơn nghỉ phép thất bại: " + e.getMessage(), null);
        }
    }

    private Employee getCurrentEmployeeForDriverOrShipper() {
        String roleName = SecurityUtils.getAuthenticatedUserRole();
        if (!"Driver".equalsIgnoreCase(roleName) && !"Shipper".equalsIgnoreCase(roleName)) {
            throw new LeaveException("Chỉ tài xế hoặc shipper mới được thao tác đơn nghỉ phép");
        }

        Integer userId = SecurityUtils.getAuthenticatedUserId();
        return employeeRepository.findByUserId(userId)
                .stream()
                .filter(e -> e.getStatus() != EmployeeStatus.LEAVE)
                .findFirst()
                .orElseThrow(() -> new LeaveException("Không tìm thấy hồ sơ nhân viên hiện tại"));
    }

    private Office getManagedOffice() {
        String roleName = SecurityUtils.getAuthenticatedUserRole();
        if (!"Manager".equalsIgnoreCase(roleName)) {
            throw new LeaveException("Chỉ quản lý bưu cục mới được thao tác chức năng này");
        }

        Integer userId = SecurityUtils.getAuthenticatedUserId();
        try {
            return employeeManagerService.getManagedOfficeByUserId(userId);
        } catch (RuntimeException e) {
            throw new LeaveException(e.getMessage());
        }
    }

    private Employee getCurrentManagerEmployeeInOffice(Integer officeId) {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        return employeeRepository.findByUserId(userId)
                .stream()
                .filter(e -> e.getOffice() != null && e.getOffice().getId().equals(officeId))
                .findFirst()
                .orElseThrow(() -> new LeaveException("Không tìm thấy hồ sơ quản lý hiện tại"));
    }

    private void validateLeaveInput(CreateLeaveRequest request) {
        if (request.getLeaveDate() == null) {
            throw new LeaveException("Ngày nghỉ không được để trống");
        }

        if (request.getLeaveDate().isBefore(LocalDate.now())) {
            throw new LeaveException("Ngày nghỉ không được nhỏ hơn ngày hiện tại");
        }

        if (request.getReasonType() == LeaveReasonType.OTHER) {
            String customReason = cleanText(request.getCustomReason());
            if (customReason == null || customReason.isBlank()) {
                throw new LeaveException("Vui lòng nhập lý do riêng khi chọn OTHER");
            }
        }
    }

    private EmployeeLeaveDto toDto(EmployeeLeaveRequest entity) {
        String employeeName = "N/A";
        if (entity.getEmployee() != null && entity.getEmployee().getUser() != null) {
            String firstName = entity.getEmployee().getUser().getFirstName();
            String lastName = entity.getEmployee().getUser().getLastName();
            employeeName = (lastName + " " + firstName).trim();
        }

        String approvedByName = null;
        if (entity.getApprovedBy() != null && entity.getApprovedBy().getUser() != null) {
            String firstName = entity.getApprovedBy().getUser().getFirstName();
            String lastName = entity.getApprovedBy().getUser().getLastName();
            approvedByName = (lastName + " " + firstName).trim();
        }

        String reasonDisplay = entity.getReasonType() != null ? entity.getReasonType().name() : "";
        if (entity.getReasonType() == LeaveReasonType.OTHER && entity.getCustomReason() != null
                && !entity.getCustomReason().isBlank()) {
            reasonDisplay = entity.getCustomReason();
        }

        return new EmployeeLeaveDto(
                entity.getId(),
                entity.getEmployee() != null ? entity.getEmployee().getId() : null,
                employeeName,
                entity.getOffice() != null ? entity.getOffice().getId() : null,
                entity.getLeaveDate(),
                entity.getShift() != null ? entity.getShift().name() : null,
                entity.getReasonType() != null ? entity.getReasonType().name() : null,
                entity.getCustomReason(),
                entity.getEmployeeNote(),
                reasonDisplay,
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null,
                approvedByName,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}