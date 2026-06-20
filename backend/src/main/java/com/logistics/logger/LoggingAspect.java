package com.logistics.logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.logistics.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        // START LOG
        log.info("START {}.{}() with args={}", className, methodName, getMaskedArgs(joinPoint));

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            // ERROR LOG
            log.error("ERROR in {}.{}() with args={} | message={}",
                    className,
                    methodName,
                    getMaskedArgs(joinPoint),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }

        long duration = System.currentTimeMillis() - startTime;

        log.info("END {}.{}() | result={} | time={}ms",
                className,
                methodName,
                result,
                duration
        );

        return result;
    }
    private String getMaskedArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // Lấy danh sách Annotation của tất cả tham số trong hàm
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            boolean isSensitive = false;

            // Kiểm tra xem tham số thứ i có gắn @Sensitive không
            for (Annotation ann : parameterAnnotations[i]) {
                if (ann instanceof Sensitive) {
                    isSensitive = true;
                    break;
                }
            }

            // Nếu có @Sensitive thì hiện ***, không thì hiện giá trị thật
            Object value = isSensitive ? "******" : args[i];
            sb.append(value);
            if (i < args.length - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}
