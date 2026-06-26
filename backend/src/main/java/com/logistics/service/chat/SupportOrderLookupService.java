package com.logistics.service.chat;

import com.logistics.entity.Order;
import com.logistics.entity.SupportTicket;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupportOrderLookupService {

    private static final Pattern TRACKING_PATTERN = Pattern.compile("(?i)\\b([a-z]{2,6})[-_ ]?(\\d{3,}[a-zA-Z0-9]*)\\b");

    private final OrderRepository orderRepository;
    private final SupportTicketRepository supportTicketRepository;

    public Optional<Order> resolveOrderForUser(Integer ticketId, Integer accountId, String message) {
        Optional<Order> byTracking = resolveByTrackingNumberFromMessage(accountId, message);
        if (byTracking.isPresent()) {
            return byTracking;
        }

        SupportTicket ticket = supportTicketRepository.findById(ticketId).orElse(null);
        if (ticket != null && "ORDER".equalsIgnoreCase(ticket.getRelatedType()) && ticket.getRelatedId() != null) {
            Optional<Order> byTicket = orderRepository.findByIdAndUserId(ticket.getRelatedId(), accountId);
            if (byTicket.isPresent()) {
                return byTicket;
            }
            return Optional.empty();
        }

        return Optional.empty();
    }

    public Optional<Order> resolveOrderFromTicket(Integer ticketId, Integer accountId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getRelatedId() == null || ticket.getRelatedType() == null) {
            return Optional.empty();
        }
        if (!"ORDER".equalsIgnoreCase(ticket.getRelatedType())) {
            return Optional.empty();
        }
        return orderRepository.findByIdAndUserId(ticket.getRelatedId(), accountId);
    }

    public Optional<Order> resolveByTrackingNumberFromMessage(Integer accountId, String message) {
        String trackingNumber = extractTrackingNumber(message);
        if (trackingNumber == null) {
            return Optional.empty();
        }
        return resolveByTrackingNumber(accountId, trackingNumber);
    }

    public Optional<Order> resolveByTrackingNumber(Integer accountId, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeTrackingNumber(trackingNumber);

        // Nếu accountId == null, tìm đơn theo trackingNumber không filter user
        if (accountId == null) {
            return orderRepository.findByTrackingNumberIgnoreCase(normalized);
        }

        // Nếu có accountId, filter theo user để bảo mật trong ticket
        return orderRepository.findByTrackingNumberAndUserId(normalized, accountId);
    }

    public String extractTrackingNumber(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = TRACKING_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String prefix = matcher.group(1).toUpperCase();
        String number = matcher.group(2);
        return prefix + number;
    }

    public boolean hasTrackingNumber(String message) {
        return extractTrackingNumber(message) != null;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        return trackingNumber.replace(" ", "").replace("_", "-").trim();
    }
}
