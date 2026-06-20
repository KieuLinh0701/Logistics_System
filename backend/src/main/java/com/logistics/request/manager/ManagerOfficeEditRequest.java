package com.logistics.request.manager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOfficeEditRequest {
    private String email;
    private String status;
    private Integer capacity;
    private String notes;
    private String openingTime;
    private String closingTime;
}
