package com.logistics.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.entity.AuditLog;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.User;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EmployeeStatus;
import com.logistics.repository.AuditLogRepository;
import com.logistics.repository.EmployeeRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserUserService userService;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        String payloadRequestBody = extractRequestBody(pjp);

        Object result = pjp.proceed();

        saveLog(userId, audit, pjp, payloadRequestBody, result);

        return result;
    }

    @Async
    protected void saveLog(
            Integer userId,
            Audit audit,
            ProceedingJoinPoint pjp,
            String payloadRequestBody,
            Object result) {
        try {
            User user = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            User shop = null;
            Office office = null;
            if (user != null) {
                shop = userService.getShop(user);
                office = employeeRepository.findByUserIdAndStatus(userId, EmployeeStatus.ACTIVE)
                        .map(Employee::getOffice)
                        .orElse(null);
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
                    .status(AuditLogStatus.SUCCESS)
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
            if (obj instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
                Object body = responseEntity.getBody();
                if (body instanceof com.logistics.response.ApiResponse<?> apiResponse) {
                    return objectMapper.writeValueAsString(apiResponse.getData());
                }
                return objectMapper.writeValueAsString(body);
            }
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("AuditAspect: could not serialize to json - {}", e.getMessage());
            return null;
        }
    }

    private String buildDescription(Audit audit) {
        return audit.action().name() + " " + audit.entity().name();
    }
}