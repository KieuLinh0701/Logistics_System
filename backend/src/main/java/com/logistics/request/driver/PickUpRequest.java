package com.logistics.request.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickUpRequest {
    private Integer vehicleId;
    private List<Integer> orderIds;
}




