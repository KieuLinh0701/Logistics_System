package com.logistics.service.chat;

import com.logistics.entity.Order;

import java.util.Optional;

public interface SupportOrderLookupService {
    Optional<Order> resolveOrderForUser(Integer ticketId, Integer accountId, String message);
    Optional<Order> resolveOrderFromTicket(Integer ticketId, Integer accountId);
    Optional<Order> resolveByTrackingNumberFromMessage(Integer accountId, String message);
    Optional<Order> resolveByTrackingNumber(Integer accountId, String trackingNumber);
    String extractTrackingNumber(String message);
    boolean hasTrackingNumber(String message);
}
