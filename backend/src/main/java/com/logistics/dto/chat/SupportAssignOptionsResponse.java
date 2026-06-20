package com.logistics.dto.chat;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAssignOptionsResponse {
    private Integer ticketId;
    private List<SupportAssignOfficeOption> suggestedOffices;
    private List<SupportAssignOfficeOption> allOffices;
}
