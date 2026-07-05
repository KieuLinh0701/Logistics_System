package com.logistics.service.chat;

import com.logistics.dto.chat.SupportAssignManagerOption;
import com.logistics.dto.chat.SupportAssignOptionsResponse;

import java.util.List;

public interface SupportAssignService {
    SupportAssignOptionsResponse getAssignOptions(Integer ticketId);
    List<SupportAssignManagerOption> getManagersByOffice(Integer officeId);
}
