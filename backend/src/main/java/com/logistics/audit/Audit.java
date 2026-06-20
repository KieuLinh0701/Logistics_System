package com.logistics.audit;

import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    EntityType entity();
    AuditLogAction action();
    String description() default "";
    String[] params() default {};
}