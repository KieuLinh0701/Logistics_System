package com.logistics.request.manager.audit;

import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogSearchRequest {
    private Integer page;
    private Integer limit;
    private String sort;
    private String startDate;
    private String endDate;
    private String search;
    private EntityType entity;
    private AuditLogAction action;
    private AuditLogStatus status;
}
