package com.logistics.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAssignOptionsResponse {
    private Integer ticketId;
    private List<SupportAssignOfficeOption> suggestedOffices;
    private List<SupportAssignOfficeOption> allOffices;
}
