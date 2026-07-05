package com.logistics.service.chat;

import com.logistics.dto.chat.SupportTicketDetailDto;
import com.logistics.dto.chat.SupportTicketDto;
import com.logistics.request.chat.AssignTicketRequest;
import com.logistics.request.chat.CloseTicketRequest;
import com.logistics.request.chat.CreateSupportTicketRequest;

import java.util.List;

public interface SupportTicketService {
    SupportTicketDto createTicket(Integer accountId, CreateSupportTicketRequest request);
    List<SupportTicketDto> getMyTickets(Integer accountId, String roleName);
    SupportTicketDetailDto getTicketDetail(Integer id, Integer accountId, String roleName);
    SupportTicketDto assignTicket(Integer ticketId, Integer accountId, AssignTicketRequest request);
    SupportTicketDto closeTicket(Integer ticketId, Integer accountId, String roleName, CloseTicketRequest request);
    SupportTicketDto forceCloseTicket(Integer ticketId, Integer accountId, CloseTicketRequest request);
    SupportTicketDto reopenTicket(Integer ticketId, Integer accountId, String roleName);
}
