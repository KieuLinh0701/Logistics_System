package com.logistics.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.entity.*;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EmployeeStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.repository.AuditLogRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.RoleRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.user.UserUserService;
import com.logistics.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserUserService userService;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        Integer userId = null;
        Role currentRole = null;

        try {
            userId = SecurityUtils.getAuthenticatedUserId();
            currentRole = SecurityUtils.getAuthenticatedUserRole();
        } catch (Exception e) {
        }

        String payloadRequestBody = extractRequestBody(pjp);

        try {
            Object result = pjp.proceed();

            saveLog(userId, currentRole, audit, pjp, payloadRequestBody, result, AuditLogStatus.SUCCESS, null);

            return result;
        } catch (Throwable e) {
            AuditLogStatus status = (e instanceof org.springframework.security.access.AccessDeniedException)
                    ? AuditLogStatus.FORBIDDEN
                    : AuditLogStatus.FAILED;

            saveLog(userId, currentRole, audit, pjp, payloadRequestBody, null, status, e.getMessage());
            throw e;
        }
    }

    @Async
    protected void saveLog(
            Integer userId,
            Role role,
            Audit audit,
            ProceedingJoinPoint pjp,
            String payloadRequestBody,
            Object result,
            AuditLogStatus status,
            String errorMessage) {
        try {
            User user = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            User shop = null;
            Office office = null;

            if (user != null) {
                try {
                    Role currentRole = roleRepository.findByIdWithPermissionGroups(role.getId())
                            .orElseThrow(() -> new AppException(AccountErrorCode.ACCOUNT_UNAUTHORIZED_ACCESS));

                    boolean isSystemRole = currentRole.getUserOwner() == null && !currentRole.getName().equalsIgnoreCase("User");

                    if (isSystemRole) {
                        office = employeeRepository.findByUserIdAndStatus(userId, EmployeeStatus.ACTIVE)
                                .map(Employee::getOffice)
                                .orElse(null);
                    } else {
                        shop = userService.getShop(user);
                    }
                } catch (Exception e) {
                    log.warn("AuditAspect: could not determine role context - {}", e.getMessage());
                }
            }

            String entityId = extractEntityId(pjp, audit);

            if (entityId == null && result != null) {
                entityId = extractIdFromObject(result);
            }

            String description = audit.description().isBlank()
                    ? buildDescription(audit)
                    : audit.description();

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .shop(shop)
                    .office(office)
                    .entity(audit.entity())
                    .action(audit.action())
                    .entityId(entityId)
                    .description(description)
                    .status(status)
                    .errorMessage(errorMessage)
                    .payloadRequestBody(payloadRequestBody)
                    .payloadResult(toJson(result))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("AuditAspect: failed to save log - {}", e.getMessage());
        }
    }

    private String extractEntityId(ProceedingJoinPoint pjp, Audit audit) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Parameter[] params = signature.getMethod().getParameters();
            Object[] args = pjp.getArgs();

            if (audit.params().length > 0) {
                Set<String> declared = Set.of(audit.params());
                List<String> values = new ArrayList<>();

                for (int i = 0; i < params.length; i++) {
                    if (declared.contains(params[i].getName())) {
                        values.add(audit.params().length == 1
                                ? String.valueOf(args[i])
                                : params[i].getName() + "=" + args[i]);
                    }
                }
                return String.join(", ", values);
            }

            for (int i = 0; i < params.length; i++) {
                String name = params[i].getName();
                if (name.equals("id") || name.equals("entityId")) {
                    return String.valueOf(args[i]);
                }
            }
        } catch (Exception e) {
            log.warn("AuditAspect: could not extract entityId - {}", e.getMessage());
        }
        return null;
    }

    private String extractRequestBody(ProceedingJoinPoint pjp) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Parameter[] params = signature.getMethod().getParameters();
            Object[] args = pjp.getArgs();

            for (int i = 0; i < params.length; i++) {
                Object arg = args[i];
                String name = params[i].getName();
                if (name.equals("userId")) continue;
                if (arg instanceof jakarta.servlet.http.HttpServletRequest) continue;
                if (arg instanceof Integer || arg instanceof Long
                        || arg instanceof String || arg instanceof Boolean) continue;
                if (arg != null) return toJson(arg);
            }
        } catch (Exception e) {
            log.warn("AuditAspect: could not extract request body - {}", e.getMessage());
        }
        return null;
    }

    private String extractIdFromObject(Object result) {
        try {
            Object target = result;
            if (target instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
                target = responseEntity.getBody();
                if (target instanceof com.logistics.response.ApiResponse<?> apiResponse) {
                    target = apiResponse.getData();
                }
            }

            if (target == null) return null;
            try {
                var method = target.getClass().getMethod("getId");
                Object idValue = method.invoke(target);
                return idValue != null ? String.valueOf(idValue) : null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (Exception e) {
            log.warn("AuditAspect: could not extract id from result - {}", e.getMessage());
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            Object target = obj;
            if (obj instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
                Object body = responseEntity.getBody();
                if (body instanceof com.logistics.response.ApiResponse<?> apiResponse) {
                    target = apiResponse.getData();
                } else {
                    target = body;
                }
            }

            // Tuyệt đối KHÔNG serialize trực tiếp Hibernate entity ra JSON -
            // có thể chứa back-reference gây StackOverflowError (vd: Shipment <-> ShipmentOrder).
            String simpleName = target != null ? target.getClass().getName() : "";
            if (isEntityClass(simpleName)) {
                return null;
            }

            return objectMapper.writeValueAsString(target);
        } catch (StackOverflowError soe) {
            // Defensive: tránh làm hỏng cả request khi serialize chạm vòng lặp entity
            log.warn("AuditAspect.toJson: StackOverflowError when serializing {} - skipping payload",
                    obj.getClass().getName());
            return null;
        } catch (Exception e) {
            log.warn("AuditAspect: could not serialize to json - {}", e.getMessage());
            return null;
        }
    }

    private boolean isEntityClass(String className) {
        if (className == null) return false;
        // Các package entity của project + Hibernate proxy thường nằm trong com.logistics.entity.*
        return className.startsWith("com.logistics.entity.");
    }

    private String buildDescription(Audit audit) {
        return audit.action().name() + " " + audit.entity().name();
    }
}