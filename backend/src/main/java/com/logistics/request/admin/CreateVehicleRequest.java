package com.logistics.request.admin;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreateVehicleRequest {
    private String licensePlate;
    private String type;
    private BigDecimal capacity;
    private String status;
    private String description;
    private Integer officeId;
}




