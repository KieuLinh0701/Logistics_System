package com.logistics.service.leave;

import com.logistics.dto.leave.EmployeeLeaveDto;
import com.logistics.entity.Employee;
import com.logistics.entity.EmployeeLeaveRequest;
import com.logistics.entity.Office;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.LeaveReasonType;
import com.logistics.enums.LeaveRequestStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.EmployeeLeaveRequestErrorCode;
import com.logistics.repository.EmployeeLeaveRequestRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.request.leave.ApproveLeaveRequest;
import com.logistics.request.leave.CreateLeaveRequest;
import com.logistics.service.manager.EmployeeManagerService;
import com.logistics.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeLeaveService {

    private final EmployeeLeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeManagerService employeeManagerService;

    @Transactional
    public EmployeeLeaveDto createLeave(CreateLeaveRequest request) {
            Employee employee = getCurrentEmployeeForDriverOrShipper();
            validateLeaveInput(request);

            Set<LeaveRequestStatus> blockingStatuses = Set.of(
                    LeaveRequestStatus.PENDING,
                    LeaveRequestStatus.APPROVED
            );
            boolean hasDuplicate = leaveRepository.existsByEmployeeIdAndLeaveDateAndShiftAndStatusIn(
                    employee.getId(),
                    request.getLeaveDate(),
                    request.getShift(),
                    blockingStatuses
            );
            if (hasDuplicate) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_DUPLICATE);
            }

            Office office = employee.getOffice();
            if (office == null) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_MISSING_OFFICE);
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
            return toDto(saved);
    }

    public List<EmployeeLeaveDto> getMyLeaves() {
            Employee employee = getCurrentEmployeeForDriverOrShipper();

            return leaveRepository.findByEmployeeId(employee.getId())
                    .stream()
                    .sorted(Comparator.comparing(EmployeeLeaveRequest::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::toDto)
                    .toList();

    }

    @Transactional
    public void cancelLeave(Integer leaveId) {
            Employee employee = getCurrentEmployeeForDriverOrShipper();

            EmployeeLeaveRequest leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_LEAVE_REQUEST_NOT_FOUND));

            if (!leave.getEmployee().getId().equals(employee.getId())) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_UNAUTHORIZED_CANCEL);
            }

            if (leave.getStatus() != LeaveRequestStatus.PENDING) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_INVALID_LEAVE_STATUS);
            }

            leave.setStatus(LeaveRequestStatus.CANCELLED);
            leaveRepository.save(leave);
    }

    public List<EmployeeLeaveDto> getOfficeLeaves() {
            Office office = getManagedOffice();

            return leaveRepository.findByOfficeId(office.getId())
                    .stream()
                    .sorted(Comparator.comparing(EmployeeLeaveRequest::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(this::toDto)
                    .toList();
    }

    @Transactional
    public EmployeeLeaveDto approveLeave(Integer leaveId, ApproveLeaveRequest request) {
            Office office = getManagedOffice();
            Employee managerEmployee = getCurrentManagerEmployeeInOffice(office.getId());

            EmployeeLeaveRequest leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_LEAVE_REQUEST_NOT_FOUND));

            if (leave.getOffice() == null || !leave.getOffice().getId().equals(office.getId())) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_UNAUTHORIZED_APPROVE);
            }

            if (leave.getStatus() != LeaveRequestStatus.PENDING) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_INVALID_LEAVE_STATUS);
            }

            if (request.getStatus() != LeaveRequestStatus.APPROVED
                    && request.getStatus() != LeaveRequestStatus.REJECTED) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_INVALID_APPROVAL_STATUS);
            }

            leave.setStatus(request.getStatus());
            leave.setApprovedBy(managerEmployee);

            EmployeeLeaveRequest saved = leaveRepository.save(leave);
            return toDto(saved);
    }

    private Employee getCurrentEmployeeForDriverOrShipper() {
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        if (!"Driver".equalsIgnoreCase(roleName) && !"Shipper".equalsIgnoreCase(roleName)) {
            throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_ROLE_REQUIRED_DRIVER_OR_SHIPPER);
        }

        Integer userId = SecurityUtils.getAuthenticatedUserId();
        return employeeRepository.findByUserId(userId)
                .stream()
                .filter(e -> e.getStatus() != EmployeeStatus.LEAVE)
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private Office getManagedOffice() {
        String roleName = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        if (!"Manager".equalsIgnoreCase(roleName)) {
            throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_ROLE_REQUIRED_MANAGER);
        }

        Integer userId = SecurityUtils.getAuthenticatedUserId();
        return employeeManagerService.getManagedOfficeByUserId(userId);
    }

    private Employee getCurrentManagerEmployeeInOffice(Integer officeId) {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        return employeeRepository.findByUserId(userId)
                .stream()
                .filter(e -> e.getOffice() != null && e.getOffice().getId().equals(officeId))
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private void validateLeaveInput(CreateLeaveRequest request) {
        if (request.getLeaveDate() == null) {
            throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_LEAVE_DATE_REQUIRED);
        }

        if (request.getLeaveDate().isBefore(LocalDate.now())) {
            throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_INVALID_LEAVE_DATE);
        }

        if (request.getReasonType() == LeaveReasonType.OTHER) {
            String customReason = cleanText(request.getCustomReason());
            if (customReason == null || customReason.isBlank()) {
                throw new AppException(EmployeeLeaveRequestErrorCode.EMPLOYEE_LEAVE_REQUEST_MISSING_CUSTOM_REASON);
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