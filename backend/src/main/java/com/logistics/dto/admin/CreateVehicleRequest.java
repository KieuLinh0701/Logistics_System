package com.logistics.dto.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateVehicleRequest {
    private String licensePlate;
    private String type;
    private BigDecimal capacity;
    private String status;
    private String description;
    private Integer officeId;
}



