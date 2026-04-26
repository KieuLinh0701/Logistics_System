package com.logistics.config;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.logistics.entity.SupportTicket;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.SupportTicketRepository;

@Component
public class UserChannelInterceptor implements ChannelInterceptor {

    private final SupportTicketRepository supportTicketRepository;
    private final AccountRepository accountRepository;

    public UserChannelInterceptor(SupportTicketRepository supportTicketRepository,
            AccountRepository accountRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            if (principal == null || principal.getName() == null || destination == null) {
                return message;
            }

            if (destination.startsWith("/topic/support/")) {
                try {
                    Integer accountId = Integer.parseInt(principal.getName());
                    String suffix = destination.substring("/topic/support/".length());
                    Integer ticketId = Integer.parseInt(suffix);

                    SupportTicket ticket = supportTicketRepository.findById(ticketId)
                            .orElseThrow(() -> new IllegalArgumentException("Support ticket not found"));

                    boolean isOwner = ticket.getCreatedByAccountId() != null && ticket.getCreatedByAccountId().equals(accountId);
                    boolean isAssigned = ticket.getAssignedToAccountId() != null && ticket.getAssignedToAccountId().equals(accountId);

                    List<String> roleNames = accountRepository.findActiveRoleNamesByAccountId(accountId);
                    boolean isAdmin = roleNames.stream().anyMatch(r -> "Admin".equalsIgnoreCase(r));

                    if (!isOwner && !isAssigned && !isAdmin) {
                        throw new IllegalArgumentException("Forbidden support subscription");
                    }
                } catch (Exception ex) {
                    throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
                }
            }
        }

        return message;
    }
}
