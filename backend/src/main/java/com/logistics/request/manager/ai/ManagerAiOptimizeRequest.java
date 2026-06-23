package com.logistics.request.manager.ai;

import com.logistics.enums.RouteMode;
import lombok.Data;

@Data
public class ManagerAiOptimizeRequest {
    private String startTime;
    private Boolean returnToOffice = true;
    private Boolean includePickupStops = false;
    private RouteMode routeMode = RouteMode.CLOSED_LOOP;
    private Integer maxOrdersPerShipper;
}
