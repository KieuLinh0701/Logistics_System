package com.logistics.service.recruitment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
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

    public JobPostingDto createJob(CreateJobPostingRequest request) {
        assertCanManageJobPosting();
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        Account createdBy = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        Office office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));

        JobPosting entity = new JobPosting();
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setRoleType(request.getRoleType());
        entity.setOffice(office);
        entity.setStatus(request.getStatus() == null ? JobPostingStatus.OPEN : request.getStatus());
        // validate new fields
        if (request.getQuantityNeeded() == null || request.getQuantityNeeded() <= 0) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_JOB_POSTING_QUANTITY_INVALID);
        }
        if (request.getShift() == null) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_JOB_POSTING_SHIFT_REQUIRED);
        }
        entity.setQuantityNeeded(request.getQuantityNeeded());
        entity.setShift(request.getShift());
        entity.setCreatedBy(createdBy);

        JobPosting saved = jobPostingRepository.save(entity);
        return RecruitmentMapper.toJobPostingDto(saved);
    }

    public ListResponse<JobPostingDto> listJobs(int page, int limit, JobPostingStatus status, Integer officeId) {
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

        return new ListResponse<>(list, pagination);
    }

    public JobPostingDto getJobById(Long id) {
        assertCanViewJobs();

        JobPosting posting = findJobPosting(id);
        return RecruitmentMapper.toJobPostingDto(posting);
    }

    public JobPostingDto updateJob(Long id, UpdateJobPostingRequest request) {
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
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));
            posting.setOffice(office);
        }
        if (request.getStatus() != null) {
            posting.setStatus(request.getStatus());
        }
        if (request.getQuantityNeeded() != null) {
            if (request.getQuantityNeeded() <= 0) {
                throw new AppException(RecruitmentErrorCode.RECRUITMENT_JOB_POSTING_QUANTITY_INVALID);
            }
            posting.setQuantityNeeded(request.getQuantityNeeded());
        }
        if (request.getShift() != null) {
            posting.setShift(request.getShift());
        }

        JobPosting saved = jobPostingRepository.save(posting);
        return RecruitmentMapper.toJobPostingDto(saved);
    }

    public void deleteJob(Long id) {
        assertCanManageJobPosting();

        JobPosting posting = findJobPosting(id);
        jobPostingRepository.delete(posting);
    }

    public JobApplicationDto createApplication(CreateJobApplicationRequest request) {
        JobPosting posting = findJobPosting(request.getJobPostingId());
        if (posting.getStatus() != JobPostingStatus.OPEN) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_JOB_POSTING_CLOSED);
        }

        boolean exists = jobApplicationRepository.existsByEmailAndJobPostingId(request.getEmail().trim(),
                request.getJobPostingId());
        if (exists) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_APPLICATION_EMAIL_DUPLICATED);
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
        return RecruitmentMapper.toJobApplicationDto(saved);
    }

    public ListResponse<JobApplicationDto> listApplications(
            int page,
            int limit,
            Long jobPostingId,
            JobApplicationStatus status) {

        String role = getCurrentRoleOrThrow();
        boolean adminOrManager = hasRole(role, "admin") || hasRole(role, "manager");
        boolean branchManager = hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");

        if (!adminOrManager && !branchManager) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
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

        return new ListResponse<>(list, pagination);
    }

    public JobApplicationDto getApplicationById(Long id) {
        JobApplication application = findJobApplication(id);

        String role = getCurrentRoleOrThrow();
        boolean adminOrManager = hasRole(role, "admin") || hasRole(role, "manager");
        boolean branchManager = hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");

        if (!adminOrManager && !branchManager) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
        }

        if (branchManager) {
            Integer officeId = resolveCurrentManagerOfficeId();
            Integer applicationOfficeId = application.getJobPosting().getOffice().getId();
            if (!officeId.equals(applicationOfficeId)) {
                throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
            }
        }

        return RecruitmentMapper.toJobApplicationDto(application);
    }

    @Transactional
    public JobApplicationDto updateApplicationStatus(Long id, UpdateJobApplicationStatusRequest request) {
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
            emailService.sendRecruitmentRejectionEmail(saved.getEmail());
        }

        return RecruitmentMapper.toJobApplicationDto(saved);
    }

    @Transactional
    public void approveApplication(Long applicationId) {
        JobApplication application = findJobApplication(applicationId);
        if (application.getStatus() != JobApplicationStatus.APPROVED) {
            throw new AppException(RecruitmentErrorCode.RECRUITMENT_APPLICATION_NOT_APPROVED);
        }

        JobPosting posting = application.getJobPosting();
        Office office = posting.getOffice();
        String targetRoleName = mapRecruitmentRoleToRoleName(posting.getRoleType());

        Role role = roleRepository.findByNameAndUserOwnerIsNull(targetRoleName)
                .orElseThrow(() -> new AppException(RoleErrorCode.ROLE_NOT_FOUND));

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

        throw new AppException(RecruitmentErrorCode.RECRUITMENT_APPLICATION_INVALID_STATUS_TRANSITION);
    }

    private void assertCanManageJobPosting() {
        String role = getCurrentRoleOrThrow();
        if (hasRole(role, "admin") || hasRole(role, "manager")) {
            return;
        }
        throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
    }

    private void assertCanReviewApplications() {
        String role = getCurrentRoleOrThrow();
        if (hasRole(role, "admin") || hasRole(role, "manager")) {
            return;
        }
        throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
    }

    private void assertCanViewJobs() {
        try {
            String role = getCurrentRoleOrThrow();
            boolean allowed = hasRole(role, "admin") || hasRole(role, "manager") || hasRole(role, "branch manager") || hasRole(role, "branch_manager");
            if (!allowed) {
                throw new AppException(RecruitmentErrorCode.RECRUITMENT_ACCESS_DENIED);
            }
        } catch (.RuntimeException ex) {
            return;
        }
    }

    private String getCurrentRoleOrThrow() {
        String role = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                .getName();
        if (role == null || role.isBlank()) {
            throw new AppException(CommonErrorCode.ROLE_INVALID);
        }
        return role;
    }

    private boolean hasRole(String currentRole, String expected) {
        return currentRole != null && currentRole.equalsIgnoreCase(expected);
    }

    private JobPosting findJobPosting(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.RECRUITMENT_JOB_POSTING_NOT_FOUND));
    }

    private JobApplication findJobApplication(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.RECRUITMENT_APPLICATION_NOT_FOUND));
    }

    private Integer resolveCurrentManagerOfficeId() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        List<Employee> employees = employeeRepository.findAllByAccountId(accountId);
        if (employees.isEmpty() || employees.getFirst().getOffice() == null) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_USER_OFFICE_MISSING);
        }
        return employees.getFirst().getOffice().getId();
    }

    private String mapRecruitmentRoleToRoleName(RecruitmentRoleType roleType) {
        return switch (roleType) {
            case DRIVER -> "Driver";
            case SHIPPER -> "Shipper";
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
                    throw new AppException(UserErrorCode.USER_PHONE_NUMBER_EXISTED);
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
            throw new AppException(UserErrorCode.USER_PHONE_NUMBER_EXISTED);
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
