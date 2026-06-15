package com.logistics.service.recruitment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.recruitment.JobApplicationDto;
import com.logistics.dto.recruitment.JobPostingDto;
import com.logistics.entity.Account;
import com.logistics.entity.AccountRole;
import com.logistics.entity.Employee;
import com.logistics.entity.JobApplication;
import com.logistics.entity.JobPosting;
import com.logistics.entity.Office;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.enums.EmployeeStatus;
import com.logistics.enums.JobApplicationStatus;
import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import com.logistics.exception.RecruitmentException;
import com.logistics.mapper.RecruitmentMapper;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.AccountRoleRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.JobApplicationRepository;
import com.logistics.repository.JobPostingRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.recruitment.CreateJobApplicationRequest;
import com.logistics.request.recruitment.CreateJobPostingRequest;
import com.logistics.request.recruitment.UpdateJobApplicationStatusRequest;
import com.logistics.request.recruitment.UpdateJobPostingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.SecurityUtils;

@Service
public class RecruitmentService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountRoleRepository accountRoleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.logistics.utils.EmailService emailService;

    public ApiResponse<JobPostingDto> createJob(CreateJobPostingRequest request) {
        assertCanManageJobPosting();
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        Account createdBy = accountRepository.findById(accountId)
                .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản tạo tin"));

        Office office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND, "Không tìm thấy bưu cục"));

        JobPosting entity = new JobPosting();
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setRoleType(request.getRoleType());
        entity.setOffice(office);
        entity.setStatus(request.getStatus() == null ? JobPostingStatus.OPEN : request.getStatus());
        // validate new fields
        if (request.getQuantityNeeded() == null || request.getQuantityNeeded() <= 0) {
            throw new RecruitmentException(HttpStatus.BAD_REQUEST, "Số lượng cần tuyển phải lớn hơn 0");
        }
        if (request.getShift() == null) {
            throw new RecruitmentException(HttpStatus.BAD_REQUEST, "Ca làm việc không được để trống");
        }
        entity.setQuantityNeeded(request.getQuantityNeeded());
        entity.setShift(request.getShift());
        entity.setCreatedBy(createdBy);

        JobPosting saved = jobPostingRepository.save(entity);
        return new ApiResponse<>(true, "Tạo tin tuyển dụng thành công", RecruitmentMapper.toJobPostingDto(saved));
    }

    public ApiResponse<ListResponse<JobPostingDto>> listJobs(int page, int limit, JobPostingStatus status, Integer officeId) {
        assertCanViewJobs();

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<JobPosting> data;

        if (officeId != null && status != null) {
            data = jobPostingRepository.findByOfficeIdAndStatusOrderByCreatedAtDesc(officeId, status, pageable);
        } else if (officeId != null) {
            data = jobPostingRepository.findByOfficeIdOrderByCreatedAtDesc(officeId, pageable);
        } else if (status != null) {
            data = jobPostingRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            data = jobPostingRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<JobPostingDto> list = data.getContent().stream().map(RecruitmentMapper::toJobPostingDto).toList();
        Pagination pagination = new Pagination((int) data.getTotalElements(), page, limit, data.getTotalPages());

        return new ApiResponse<>(true, "Lấy danh sách tin tuyển dụng thành công", new ListResponse<>(list, pagination));
    }

    public ApiResponse<JobPostingDto> getJobById(Long id) {
        assertCanViewJobs();

        JobPosting posting = findJobPosting(id);
        return new ApiResponse<>(true, "Lấy chi tiết tin tuyển dụng thành công", RecruitmentMapper.toJobPostingDto(posting));
    }

    public ApiResponse<JobPostingDto> updateJob(Long id, UpdateJobPostingRequest request) {
        assertCanManageJobPosting();

        JobPosting posting = findJobPosting(id);

        if (request.getTitle() != null) {
            posting.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            posting.setDescription(request.getDescription().trim());
        }
        if (request.getRoleType() != null) {
            posting.setRoleType(request.getRoleType());
        }
        if (request.getOfficeId() != null) {
            Office office = officeRepository.findById(request.getOfficeId())
                    .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND, "Không tìm thấy bưu cục"));
            posting.setOffice(office);
        }
        if (request.getStatus() != null) {
            posting.setStatus(request.getStatus());
        }
        if (request.getQuantityNeeded() != null) {
            if (request.getQuantityNeeded() <= 0) {
                throw new RecruitmentException(HttpStatus.BAD_REQUEST, "Số lượng cần tuyển phải lớn hơn 0");
            }
            posting.setQuantityNeeded(request.getQuantityNeeded());
        }
        if (request.getShift() != null) {
            posting.setShift(request.getShift());
        }

        JobPosting saved = jobPostingRepository.save(posting);
        return new ApiResponse<>(true, "Cập nhật tin tuyển dụng thành công", RecruitmentMapper.toJobPostingDto(saved));
    }

    public ApiResponse<String> deleteJob(Long id) {
        assertCanManageJobPosting();

        JobPosting posting = findJobPosting(id);
        jobPostingRepository.delete(posting);
        return new ApiResponse<>(true, "Xóa tin tuyển dụng thành công", null);
    }

    public ApiResponse<JobApplicationDto> createApplication(CreateJobApplicationRequest request) {
        JobPosting posting = findJobPosting(request.getJobPostingId());
        if (posting.getStatus() != JobPostingStatus.OPEN) {
            throw new RecruitmentException(HttpStatus.BAD_REQUEST, "Tin tuyển dụng đã đóng");
        }

        boolean exists = jobApplicationRepository.existsByEmailAndJobPostingId(request.getEmail().trim(),
                request.getJobPostingId());
        if (exists) {
            throw new RecruitmentException(HttpStatus.CONFLICT, "Email này đã nộp hồ sơ cho tin tuyển dụng này");
        }

        JobApplication entity = new JobApplication();
        entity.setJobPosting(posting);
        entity.setFullName(request.getFullName().trim());
        entity.setPhone(request.getPhone().trim());
        entity.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        entity.setAddress(request.getAddress().trim());
        entity.setCvUrl(request.getCvUrl().trim());
        entity.setStatus(JobApplicationStatus.PENDING);

        JobApplication saved = jobApplicationRepository.save(entity);
        return new ApiResponse<>(true, "Nộp hồ sơ thành công", RecruitmentMapper.toJobApplicationDto(saved));
    }

    public ApiResponse<ListResponse<JobApplicationDto>> listApplications(
            int page,
            int limit,
            Long jobPostingId,
            JobApplicationStatus status) {

        String role = getCurrentRoleOrThrow();
        boolean adminOrManager = hasRole(role, "admin") || hasRole(role, "manager");
        boolean branchManager = hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");

        if (!adminOrManager && !branchManager) {
            throw new RecruitmentException(HttpStatus.FORBIDDEN, "Không có quyền xem hồ sơ tuyển dụng");
        }

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<JobApplication> data;

        if (adminOrManager) {
            if (jobPostingId != null && status != null) {
                JobPosting posting = findJobPosting(jobPostingId);
                data = jobApplicationRepository.findByJobPostingOfficeIdAndStatusOrderByCreatedAtDesc(posting.getOffice().getId(),
                        status, pageable);
            } else if (jobPostingId != null) {
                data = jobApplicationRepository.findByJobPostingIdOrderByCreatedAtDesc(jobPostingId, pageable);
            } else if (status != null) {
                data = jobApplicationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
            } else {
                data = jobApplicationRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            Integer officeId = resolveCurrentManagerOfficeId();
            if (status != null) {
                data = jobApplicationRepository.findByJobPostingOfficeIdAndStatusOrderByCreatedAtDesc(officeId, status,
                        pageable);
            } else {
                data = jobApplicationRepository.findByJobPostingOfficeIdOrderByCreatedAtDesc(officeId, pageable);
            }
        }

        List<JobApplicationDto> list = data.getContent().stream().map(RecruitmentMapper::toJobApplicationDto).toList();
        Pagination pagination = new Pagination((int) data.getTotalElements(), page, limit, data.getTotalPages());

        return new ApiResponse<>(true, "Lấy danh sách hồ sơ thành công", new ListResponse<>(list, pagination));
    }

    public ApiResponse<JobApplicationDto> getApplicationById(Long id) {
        JobApplication application = findJobApplication(id);

        String role = getCurrentRoleOrThrow();
        boolean adminOrManager = hasRole(role, "admin") || hasRole(role, "manager");
        boolean branchManager = hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");

        if (!adminOrManager && !branchManager) {
            throw new RecruitmentException(HttpStatus.FORBIDDEN, "Không có quyền xem hồ sơ tuyển dụng");
        }

        if (branchManager) {
            Integer officeId = resolveCurrentManagerOfficeId();
            Integer applicationOfficeId = application.getJobPosting().getOffice().getId();
            if (!officeId.equals(applicationOfficeId)) {
                throw new RecruitmentException(HttpStatus.FORBIDDEN, "Chỉ được xem hồ sơ thuộc bưu cục của bạn");
            }
        }

        return new ApiResponse<>(true, "Lấy chi tiết hồ sơ thành công", RecruitmentMapper.toJobApplicationDto(application));
    }

    @Transactional
    public ApiResponse<JobApplicationDto> updateApplicationStatus(Long id, UpdateJobApplicationStatusRequest request) {
        assertCanReviewApplications();

        JobApplication application = findJobApplication(id);
        JobApplicationStatus current = application.getStatus();
        JobApplicationStatus next = request.getStatus();

        validateTransition(current, next);

        application.setStatus(next);
        JobApplication saved = jobApplicationRepository.save(application);

        if (next == JobApplicationStatus.APPROVED) {
            approveApplication(saved.getId());
        } else if (next == JobApplicationStatus.REJECTED) {
            try {
                emailService.sendRecruitmentRejectionEmail(saved.getEmail());
            } catch (Exception ex) {
                
            }
        }

        return new ApiResponse<>(true, "Cập nhật trạng thái hồ sơ thành công", RecruitmentMapper.toJobApplicationDto(saved));
    }

    @Transactional
    public void approveApplication(Long applicationId) {
        JobApplication application = findJobApplication(applicationId);
        if (application.getStatus() != JobApplicationStatus.APPROVED) {
            throw new RecruitmentException(HttpStatus.BAD_REQUEST, "Hồ sơ chưa ở trạng thái APPROVED");
        }

        JobPosting posting = application.getJobPosting();
        Office office = posting.getOffice();
        String targetRoleName = mapRecruitmentRoleToRoleName(posting.getRoleType());

        Role role = roleRepository.findByNameAndUserOwnerIsNull(targetRoleName)
                .or(() -> roleRepository.findByName(targetRoleName))
                .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy role hệ thống: " + targetRoleName));

        String tempPassword = null;
        Account account = accountRepository.findByEmail(application.getEmail().toLowerCase(Locale.ROOT)).orElse(null);
        if (account == null) {
            AccountWithTemp awt = createAccountForCandidateWithTemp(application.getEmail().toLowerCase(Locale.ROOT));
            account = awt.account();
            tempPassword = awt.tempPassword();
        }

        final Account accountForLambda = account;

        User user = resolveOrCreateUser(account, application);

        AccountRole accountRole = accountRoleRepository.findByAccountIdAndRoleId(account.getId(), role.getId())
                .orElseGet(() -> {
                    AccountRole created = new AccountRole();
                    created.setAccount(accountForLambda);
                    created.setRole(role);
                    created.setIsActive(true);
                    return accountRoleRepository.save(created);
                });

        accountRole.setIsActive(true);
        accountRoleRepository.save(accountRole);

        employeeRepository.findByAccountRoleId(accountRole.getId()).ifPresentOrElse(existing -> {
            existing.setOffice(office);
            existing.setUser(user);
            existing.setStatus(EmployeeStatus.ACTIVE);
            employeeRepository.save(existing);
        }, () -> {
            Employee employee = new Employee();
            employee.setOffice(office);
            employee.setUser(user);
            employee.setAccountRole(accountRole);
            employee.setHireDate(LocalDateTime.now());
            employee.setStatus(EmployeeStatus.ACTIVE);
            employeeRepository.save(employee);
        });

        Integer currentQty = posting.getQuantityNeeded() == null ? 0 : posting.getQuantityNeeded();
        if (currentQty > 0) {
            posting.setQuantityNeeded(currentQty - 1);
            if (posting.getQuantityNeeded() <= 0) {
                posting.setStatus(JobPostingStatus.CLOSED);
            }
            jobPostingRepository.save(posting);
        }

        // Nếu hệ thống vừa tạo account mới (có mật khẩu tạm), gửi email thông báo tài khoản
        if (tempPassword != null) {
            try {
                String positionDisplay = "";
                if (posting.getRoleType() != null) {
                    positionDisplay = switch (posting.getRoleType()) {
                        case DRIVER -> "Tài xế";
                        case SHIPPER -> "Nhân viên giao hàng";
                        case WAREHOUSE_STAFF -> "Nhân viên kho";
                        case RECONCILIATION_STAFF -> "Nhân viên đối soát";
                    };
                }

                emailService.sendRecruitmentAccountEmail(
                        accountForLambda.getEmail(),
                        tempPassword,
                        user.getFirstName() == null ? "" : user.getFirstName(),
                        user.getLastName() == null ? "" : user.getLastName(),
                        positionDisplay,
                        office == null || office.getName() == null ? "" : office.getName(),
                        posting.getShift() == null ? "" : posting.getShift().toString(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                );
            } catch (Exception ex) {
                
            }
        }
    }

    private void validateTransition(JobApplicationStatus current, JobApplicationStatus next) {
        if (current == JobApplicationStatus.PENDING && next == JobApplicationStatus.REVIEWING) {
            return;
        }
        if (current == JobApplicationStatus.REVIEWING
                && (next == JobApplicationStatus.APPROVED || next == JobApplicationStatus.REJECTED)) {
            return;
        }

        throw new RecruitmentException(HttpStatus.BAD_REQUEST,
                "Chuyển trạng thái không hợp lệ: " + current + " -> " + next);
    }

    private void assertCanManageJobPosting() {
        String role = getCurrentRoleOrThrow();
        if (hasRole(role, "admin") || hasRole(role, "manager")) {
            return;
        }
        throw new RecruitmentException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý tin tuyển dụng");
    }

    private void assertCanReviewApplications() {
        String role = getCurrentRoleOrThrow();
        if (hasRole(role, "admin") || hasRole(role, "manager")) {
            return;
        }
        throw new RecruitmentException(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt hồ sơ");
    }

    private void assertCanViewJobs() {
        try {
            String role = getCurrentRoleOrThrow();
            boolean allowed = hasRole(role, "admin") || hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");
            if (!allowed) {
                throw new RecruitmentException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem tin tuyển dụng");
            }
        } catch (RuntimeException ex) {
            return;
        }
    }

    private String getCurrentRoleOrThrow() {
        String role = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        if (role == null || role.isBlank()) {
            throw new RecruitmentException(HttpStatus.UNAUTHORIZED, "Không xác định được role hiện tại");
        }
        return role;
    }

    private boolean hasRole(String currentRole, String expected) {
        return currentRole != null && currentRole.equalsIgnoreCase(expected);
    }

    private JobPosting findJobPosting(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));
    }

    private JobApplication findJobApplication(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RecruitmentException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ ứng tuyển"));
    }

    private Integer resolveCurrentManagerOfficeId() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        List<Employee> employees = employeeRepository.findAllByAccountId(accountId);
        if (employees.isEmpty() || employees.get(0).getOffice() == null) {
            throw new RecruitmentException(HttpStatus.BAD_REQUEST,
                    "Không xác định được bưu cục của Branch Manager hiện tại");
        }
        return employees.get(0).getOffice().getId();
    }

    private String mapRecruitmentRoleToRoleName(RecruitmentRoleType roleType) {
        return switch (roleType) {
            case DRIVER -> "Driver";
            case SHIPPER -> "Shipper";
            case WAREHOUSE_STAFF -> "WarehouseStaff";
            case RECONCILIATION_STAFF -> "ReconciliationStaff";
        };
    }

    private Account createAccountForCandidate(String email) {
        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(generateTemporaryPassword()));
        account.setIsActive(true);
        account.setIsVerified(true);
        return accountRepository.save(account);
    }

    private record AccountWithTemp(Account account, String tempPassword) {
    }

    private AccountWithTemp createAccountForCandidateWithTemp(String email) {
        String temp = generateTemporaryPassword();
        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(temp));
        account.setIsActive(true);
        account.setIsVerified(true);
        Account saved = accountRepository.save(account);
        return new AccountWithTemp(saved, temp);
    }

    private User resolveOrCreateUser(Account account, JobApplication application) {
        if (account.getUser() != null) {
            User existing = account.getUser();
            if (existing.getPhoneNumber() == null || existing.getPhoneNumber().isBlank()) {
                if (userRepository.existsByPhoneNumber(application.getPhone())) {
                    throw new RecruitmentException(HttpStatus.CONFLICT,
                            "Số điện thoại đã được sử dụng bởi người dùng khác");
                }
                existing.setPhoneNumber(application.getPhone());
            }
            if (existing.getFirstName() == null || existing.getFirstName().isBlank()) {
                NameParts parts = splitFullName(application.getFullName());
                existing.setFirstName(parts.firstName());
                existing.setLastName(parts.lastName());
            }
            return userRepository.save(existing);
        }

        if (userRepository.existsByPhoneNumber(application.getPhone())) {
            throw new RecruitmentException(HttpStatus.CONFLICT,
                    "Số điện thoại đã được sử dụng, không thể tạo user mới");
        }

        NameParts parts = splitFullName(application.getFullName());
        User user = new User();
        user.setAccount(account);
        user.setFirstName(parts.firstName());
        user.setLastName(parts.lastName());
        user.setPhoneNumber(application.getPhone());
        return userRepository.save(user);
    }

    private NameParts splitFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return new NameParts("Unknown", "Candidate");
        }

        List<String> parts = Arrays.stream(normalized.split(" ")).toList();
        if (parts.size() == 1) {
            return new NameParts(parts.get(0), parts.get(0));
        }

        String firstName = parts.get(parts.size() - 1);
        String lastName = String.join(" ", parts.subList(0, parts.size() - 1));
        return new NameParts(firstName, lastName);
    }

    private String generateTemporaryPassword() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "Tmp@" + suffix + "1";
    }

    private record NameParts(String firstName, String lastName) {
    }
}
