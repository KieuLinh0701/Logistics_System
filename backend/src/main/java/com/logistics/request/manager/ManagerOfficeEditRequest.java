package com.logistics.request.manager;

import lombok.*;

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
