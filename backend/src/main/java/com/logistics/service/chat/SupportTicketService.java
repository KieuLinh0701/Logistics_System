package com.logistics.service.chat;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.Account;
import com.logistics.repository.AccountRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.response.ApiResponse;
import com.logistics.request.chat.CreateSupportTicketRequest;
import com.logistics.dto.chat.SupportTicketDetailDto;
import com.logistics.dto.chat.SupportTicketDto;
import com.logistics.entity.SupportTicket;
import com.logistics.repository.SupportTicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportTicketService {
    private final SupportTicketRepository supportTicketRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final SupportMessageService supportMessageService;

    @Transactional
    public ApiResponse<SupportTicketDto> createTicket(Integer accountId, String roleName, CreateSupportTicketRequest request) {
        try {
            if (!isUserRole(roleName)) {
                return new ApiResponse<>(false, "Chỉ User mới được tạo ticket hỗ trợ", null);
            }

            SupportTicket ticket = new SupportTicket();
            ticket.setCreatedByAccountId(accountId);
            ticket.setCode("SUP-TMP");

            Integer managerId = request.getManagerAccountId();
            Integer relatedOrderId = null;
            String relatedType = null;

            // Nếu frontend không cung cấp manager, tự động phân công dựa trên bưu cục của đơn hàng gần nhất
            if (managerId == null) {
                try {
                    var accOpt = accountRepository.findById(accountId);
                    if (accOpt.isPresent() && accOpt.get().getUser() != null) {
                        Integer userId = accOpt.get().getUser().getId();
                        var optOrder = orderRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
                        if (optOrder.isPresent()) {
                            var order = optOrder.get();
                            relatedOrderId = order.getId();
                            relatedType = "ORDER";

                            Integer officeId = null;
                            if (order.getFromOffice() != null) officeId = order.getFromOffice().getId();
                            else if (order.getToOffice() != null) officeId = order.getToOffice().getId();

                            if (officeId != null) {
                                var emps = employeeRepository.findByOfficeId(officeId);
                                List<Account> candidates = emps.stream()
                                        .map(e -> accountRepository.findByUser(e.getUser()).orElse(null))
                                        .filter(a -> a != null)
                                        .filter(a -> a.getAccountRoles() != null && a.getAccountRoles().stream()
                                                .anyMatch(ar -> Boolean.TRUE.equals(ar.getIsActive()) && ("Manager".equalsIgnoreCase(ar.getRole().getName()) || "Admin".equalsIgnoreCase(ar.getRole().getName()))))
                                        .toList();

                                if (!candidates.isEmpty()) {
                                    managerId = candidates.stream()
                                            .min((a, b) -> Long.compare(
                                                    a.getAccountRoles().stream().filter(ar -> Boolean.TRUE.equals(ar.getIsActive())).count(),
                                                    b.getAccountRoles().stream().filter(ar -> Boolean.TRUE.equals(ar.getIsActive())).count()))
                                            .map(a -> a.getId()).orElse(null);
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            ticket.setAssignedToAccountId(managerId);
            ticket.setRelatedId(relatedOrderId);
            ticket.setRelatedType(relatedType);

            ticket = supportTicketRepository.save(ticket);
            
            // Sau khi có ID, cập nhật mã ticket chính thức dựa trên ID 
            String finalCode = String.format("SUP-%06d", ticket.getId());
            ticket.setCode(finalCode);
            try {
                supportTicketRepository.save(ticket);
            } catch (Exception ex) {
                throw ex;
            }

            if (managerId != null) {
                supportMessageService.createSystemMessage(ticket.getId(), "Ticket được phân cho quản lý");
            }
            supportMessageService.createInitialMessage(ticket.getId(), accountId, request.getInitialMessage());

            SupportTicketDto dto = new SupportTicketDto();
            dto.setId(ticket.getId());
            dto.setCode(ticket.getCode());
            dto.setCreatedByAccountId(ticket.getCreatedByAccountId());
            dto.setAssignedToAccountId(ticket.getAssignedToAccountId());
            dto.setCreatedAt(ticket.getCreatedAt());
            dto.setUpdatedAt(ticket.getUpdatedAt());

            return new ApiResponse<>(true, "Tạo ticket thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Tạo ticket thất bại: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<SupportTicketDto>> getMyTickets(Integer accountId, String roleName) {
        try {
            List<SupportTicket> tickets;
            if (roleName != null && roleName.equalsIgnoreCase("Admin")) {
                tickets = supportTicketRepository.findAllByOrderByUpdatedAtDesc();
            } else if (roleName != null && roleName.equalsIgnoreCase("Manager")) {
                tickets = supportTicketRepository.findByAssignedToAccountIdOrderByUpdatedAtDesc(accountId);
            } else {
                tickets = supportTicketRepository.findByCreatedByAccountIdOrderByUpdatedAtDesc(accountId);
            }

            List<SupportTicketDto> data = tickets.stream().map(this::toTicketDto).toList();
            return new ApiResponse<>(true, "Lấy danh sách ticket thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lấy danh sách ticket thất bại: " + e.getMessage(), null);
        }
    }

    public ApiResponse<SupportTicketDetailDto> getTicketDetail(Integer id, Integer accountId, String roleName) {
        try {
            SupportTicket ticket = supportTicketRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ticket không tồn tại"));

            boolean allowed = false;
            if (roleName != null && roleName.equalsIgnoreCase("Admin")) allowed = true;
            else if (roleName != null && roleName.equalsIgnoreCase("Manager") && ticket.getAssignedToAccountId() != null && ticket.getAssignedToAccountId().equals(accountId)) allowed = true;
            else if (Objects.equals(ticket.getCreatedByAccountId(), accountId)) allowed = true;

            if (!allowed) return new ApiResponse<>(false, "Bạn không có quyền xem ticket này", null);

            SupportTicketDetailDto dto = new SupportTicketDetailDto();
            dto.setTicket(toTicketDto(ticket));
            return new ApiResponse<>(true, "Lấy chi tiết ticket thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lấy chi tiết ticket thất bại: " + e.getMessage(), null);
        }
    }

    private SupportTicketDto toTicketDto(SupportTicket t) {
        SupportTicketDto dto = new SupportTicketDto();
        dto.setId(t.getId());
        dto.setCode(t.getCode());
        dto.setCreatedByAccountId(t.getCreatedByAccountId());
        dto.setAssignedToAccountId(t.getAssignedToAccountId());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());

        try {
            if (t.getCreatedByAccountId() != null) {
                accountRepository.findById(t.getCreatedByAccountId()).ifPresent(acc -> {
                    if (acc.getUser() != null) dto.setCreatedByName(acc.getUser().getFullName());
                    dto.setCreatedByImage(acc.getUser() != null ? acc.getUser().getImages() : null);
                });
            }
            if (t.getAssignedToAccountId() != null) {
                accountRepository.findById(t.getAssignedToAccountId()).ifPresent(acc -> dto.setAssignedToName(acc.getUser() != null ? acc.getUser().getFullName() : acc.getEmail()));
            }
        } catch (Exception ignore) {}

        return dto;
    }

    private boolean isUserRole(String roleName) {
        return "User".equalsIgnoreCase(roleName);
    }
}
