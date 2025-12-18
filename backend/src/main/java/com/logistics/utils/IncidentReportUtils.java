package com.logistics.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.logistics.entity.Office;
import com.logistics.enums.IncidentStatus;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShippingRequestType;

public class IncidentReportUtils {

    private static final Map<IncidentStatus, Set<IncidentStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map.of(
        IncidentStatus.PENDING, Set.of(
            IncidentStatus.PROCESSING,
            IncidentStatus.RESOLVED, 
            IncidentStatus.REJECTED
        ),
        IncidentStatus.PROCESSING, Set.of(
            IncidentStatus.RESOLVED,
            IncidentStatus.REJECTED
        )
    );

    public static boolean canManagerChangeStatus(IncidentStatus currentStatus, IncidentStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        return MANAGER_ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }
}