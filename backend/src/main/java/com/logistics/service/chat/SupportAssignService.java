package com.logistics.service.chat;

import com.logistics.dto.chat.SupportAssignManagerOption;
import com.logistics.dto.chat.SupportAssignOfficeOption;
import com.logistics.dto.chat.SupportAssignOptionsResponse;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.SupportTicket;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportAssignService {

    private final SupportTicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;

    @Transactional(readOnly = true)
    public SupportAssignOptionsResponse getAssignOptions(Integer ticketId) {
        List<SupportAssignOfficeOption> suggestedOffices = new ArrayList<>();
        List<SupportAssignOfficeOption> allOffices = new ArrayList<>();

        try {
            // 1. Lấy ticket
            SupportTicket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) {
                log.warn("Ticket not found: {}", ticketId);
            }

            // 2. Lấy tất cả bưu cục đang hoạt động
            allOffices = getActiveOffices();

            // 3. Lấy bưu cục gợi ý nếu ticket tồn tại
            if (ticket != null) {
                suggestedOffices = getSuggestedOffices(ticket);
            }

        } catch (Exception e) {
            log.error("Error getting assign options for ticket {}: {}", ticketId, e.getMessage(), e);
            if (allOffices.isEmpty()) {
                allOffices = getActiveOffices();
            }
        }

        return SupportAssignOptionsResponse.builder()
                .ticketId(ticketId)
                .suggestedOffices(suggestedOffices)
                .allOffices(allOffices.isEmpty() ? null : allOffices)
                .build();
    }

    private List<SupportAssignOfficeOption> getActiveOffices() {
        try {
            return officeRepository.findAll().stream()
                    .filter(office -> office.getName() != null)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(Objects.toString(b.getName(), "")))
                    .map(office -> SupportAssignOfficeOption.builder()
                            .id(office.getId())
                            .name(office.getName())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting active offices: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<SupportAssignOfficeOption> getSuggestedOffices(SupportTicket ticket) {
        List<SupportAssignOfficeOption> suggested = new ArrayList<>();

        try {
            // Kiểm tra đơn hàng liên quan trước
            if ("ORDER".equalsIgnoreCase(ticket.getRelatedType()) && ticket.getRelatedId() != null) {
                List<SupportAssignOfficeOption> fromOrder = getSuggestedOfficesFromOrder(ticket.getRelatedId());
                suggested.addAll(fromOrder);
            }

            // Nếu không có gợi ý, thử lấy bưu cục từ đơn hàng cũ của user
            if (suggested.isEmpty() && ticket.getCreatedByAccountId() != null) {
                List<SupportAssignOfficeOption> fromUserOrders = getSuggestedOfficesFromUserOrders(ticket.getCreatedByAccountId());
                suggested.addAll(fromUserOrders);
            }

        } catch (Exception e) {
            log.error("Error getting suggested offices for ticket {}: {}", ticket.getId(), e.getMessage(), e);
        }

        return suggested;
    }

    private List<SupportAssignOfficeOption> getSuggestedOfficesFromOrder(Integer orderId) {
        List<SupportAssignOfficeOption> suggestions = new ArrayList<>();

        try {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getFromOffice() != null && order.getFromOffice().getName() != null) {
                    suggestions.add(SupportAssignOfficeOption.builder()
                            .id(order.getFromOffice().getId())
                            .name(order.getFromOffice().getName())
                            .build());
                }
                if (order.getToOffice() != null && order.getToOffice().getName() != null) {
                    suggestions.add(SupportAssignOfficeOption.builder()
                            .id(order.getToOffice().getId())
                            .name(order.getToOffice().getName())
                            .build());
                }
            });
        } catch (Exception e) {
            log.error("Error getting offices from order {}: {}", orderId, e.getMessage(), e);
        }

        return suggestions;
    }

    private List<SupportAssignOfficeOption> getSuggestedOfficesFromUserOrders(Integer accountId) {
        List<SupportAssignOfficeOption> suggestions = new ArrayList<>();

        try {
            // Lấy các bưu cục riêng biệt từ đơn hàng của user
            List<Office> userOffices = new ArrayList<>();

            try {
                userOffices.addAll(orderRepository.findDistinctFromOfficesByUserId(accountId));
            } catch (Exception e) {
                log.warn("Error getting from offices for user {}: {}", accountId, e.getMessage());
            }

            try {
                userOffices.addAll(orderRepository.findDistinctToOfficesByUserId(accountId));
            } catch (Exception e) {
                log.warn("Error getting to offices for user {}: {}", accountId, e.getMessage());
            }

            Set<Integer> addedIds = new HashSet<>();
            for (Office office : userOffices) {
                if (office != null && office.getId() != null && office.getName() != null) {
                    if (addedIds.add(office.getId())) {
                        suggestions.add(SupportAssignOfficeOption.builder()
                                .id(office.getId())
                                .name(office.getName())
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error getting offices from user orders {}: {}", accountId, e.getMessage(), e);
        }

        return suggestions;
    }

    @Transactional(readOnly = true)
    public List<SupportAssignManagerOption> getManagersByOffice(Integer officeId) {
        List<SupportAssignManagerOption> managers = new ArrayList<>();

        try {
            List<Employee> employees = employeeRepository.findActiveManagersByOfficeId(officeId);

            managers = employees.stream()
                    .filter(e -> e != null && e.getUser() != null)
                    .map(e -> {
                        try {
                            return SupportAssignManagerOption.builder()
                                    .accountId(e.getUser().getAccount() != null ? e.getUser().getAccount().getId() : null)
                                    .fullName(e.getUser().getFullName())
                                    .email(e.getUser().getAccount() != null ? e.getUser().getAccount().getEmail() : null)
                                    .phone(e.getUser().getPhoneNumber())
                                    .build();
                        } catch (Exception ex) {
                            log.warn("Error mapping manager employee: {}", ex.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting managers for office {}: {}", officeId, e.getMessage(), e);
        }

        return managers;
    }
}
