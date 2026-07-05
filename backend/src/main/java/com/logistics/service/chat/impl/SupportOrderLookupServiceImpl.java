package com.logistics.service.chat.impl;

import com.logistics.entity.Order;
import com.logistics.entity.SupportTicket;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SupportTicketRepository;
import com.logistics.service.chat.SupportOrderLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupportOrderLookupServiceImpl implements SupportOrderLookupService {

    private static final Pattern TRACKING_PATTERN = Pattern.compile("(?i)\\b([a-z]{2,6})[-_ ]?(\\d{3,}[a-zA-Z0-9]*)\\b");

    private final OrderRepository orderRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Override
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

    @Override
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

    @Override
    public Optional<Order> resolveByTrackingNumberFromMessage(Integer accountId, String message) {
        String trackingNumber = extractTrackingNumber(message);
        if (trackingNumber == null) {
            return Optional.empty();
        }
        return resolveByTrackingNumber(accountId, trackingNumber);
    }

    @Override
    public Optional<Order> resolveByTrackingNumber(Integer accountId, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeTrackingNumber(trackingNumber);

        if (accountId == null) {
            return orderRepository.findByTrackingNumberIgnoreCase(normalized);
        }

        return orderRepository.findByTrackingNumberAndUserId(normalized, accountId);
    }

    @Override
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

    @Override
    public boolean hasTrackingNumber(String message) {
        return extractTrackingNumber(message) != null;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        return trackingNumber.replace(" ", "").replace("_", "-").trim();
    }
}
