package com.logistics.service.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.dto.chat.InternalChatMessageDto;
import com.logistics.dto.chat.InternalChatRoomDto;
import com.logistics.entity.Employee;
import com.logistics.entity.InternalChatMessage;
import com.logistics.entity.InternalChatRoom;
import com.logistics.entity.Office;
import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.InternalChatErrorCode;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.InternalChatMessageRepository;
import com.logistics.repository.InternalChatRoomRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.chat.SendInternalChatMessageRequest;
import com.logistics.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalChatService {

    private static final String ROLE_SHIPPER = "Shipper";
    private static final String ROLE_DRIVER = "Driver";
    private static final String ROLE_MANAGER = "Manager";
    private static final String ROLE_ADMIN = "Admin";

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final InternalChatRoomRepository roomRepository;
    private final InternalChatMessageRepository messageRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Cloudinary cloudinary;

    @Transactional
    public InternalChatRoomDto getOrCreateMyRoom() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        if (!ROLE_SHIPPER.equalsIgnoreCase(roleName) && !ROLE_DRIVER.equalsIgnoreCase(roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        return roomRepository.findByEmployeeAccountId(accountId)
                .map(this::toRoomDto)
                .orElseGet(() -> createRoomForEmployee(accountId, roleName));
    }

    private InternalChatRoomDto createRoomForEmployee(Integer employeeAccountId, String roleName) {
        Employee employee = employeeRepository.findAllByAccountId(employeeAccountId)
                .stream()
                .filter(e -> {
                    String empRole = e.getAccountRole() != null && e.getAccountRole().getRole() != null
                            ? e.getAccountRole().getRole().getName() : null;
                    return ROLE_SHIPPER.equalsIgnoreCase(empRole) || ROLE_DRIVER.equalsIgnoreCase(empRole);
                })
                .findFirst()
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE));

        User user = employee.getUser();
        String employeeName = user != null ? user.getFullName() : "Nhân viên";
        Office office = employee.getOffice();
        Integer officeId = office.getId();
        String officeName = office.getName();

        Employee managerEmployee = findManagerForOffice(office);

        InternalChatRoom room = new InternalChatRoom();
        room.setEmployeeAccountId(employeeAccountId);
        room.setEmployeeName(employeeName);
        room.setEmployeeRole(roleName);
        room.setManagerAccountId(managerEmployee.getAccountRole().getAccount().getId());
        room.setOfficeId(officeId);
        room.setOfficeName(officeName);

        User managerUser = managerEmployee.getUser();
        if (managerUser != null) {
            room.setManagerName(managerUser.getFullName());
        } else {
            room.setManagerName("Quản lý");
        }

        room = roomRepository.save(room);
        return toRoomDto(room);
    }

    private Employee findManagerForOffice(Office office) {
        if (office.getManager() != null) {
            return office.getManager();
        }

        List<Employee> managers = employeeRepository.findActiveManagersByOfficeId(office.getId());
        if (!managers.isEmpty()) {
            return managers.get(0);
        }

        throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_MANAGER_NOT_FOUND);
    }

    public List<InternalChatRoomDto> getOfficeRooms() {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        List<InternalChatRoomDto> rooms;
        if (ROLE_ADMIN.equalsIgnoreCase(roleName)) {
            // Admin: lấy tất cả phòng chat
            rooms = roomRepository.findAll().stream()
                    .map(this::toRoomDto)
                    .collect(Collectors.toList());
        } else if (ROLE_MANAGER.equalsIgnoreCase(roleName)) {
            // Manager: lấy tất cả Shipper + Driver trong bưu cục, tự động tạo phòng nếu cần
            rooms = getOrCreateRoomsForOfficeEmployees(accountId);
        } else {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        return rooms;
    }

    private List<InternalChatRoomDto> getOrCreateRoomsForOfficeEmployees(Integer managerAccountId) {
        // Tìm bưu cục của quản lý
        List<Employee> managerEmployees = employeeRepository.findAllByAccountId(managerAccountId);
        Employee managerEmployee = managerEmployees.stream()
                .filter(e -> {
                    String empRole = e.getAccountRole() != null && e.getAccountRole().getRole() != null
                            ? e.getAccountRole().getRole().getName() : null;
                    return ROLE_MANAGER.equalsIgnoreCase(empRole);
                })
                .findFirst()
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_MANAGER_INFO_NOT_FOUND));

        Office office = managerEmployee.getOffice();
        Integer officeId = office.getId();

        // Lấy tất cả Shipper và Driver trong bưu cục này
        List<Employee> employees = employeeRepository.findByOfficeId(officeId);
        List<InternalChatRoomDto> rooms = new java.util.ArrayList<>();

        for (Employee emp : employees) {
            String empRole = emp.getAccountRole() != null && emp.getAccountRole().getRole() != null
                    ? emp.getAccountRole().getRole().getName() : null;

            if (empRole == null || (!ROLE_SHIPPER.equalsIgnoreCase(empRole) && !ROLE_DRIVER.equalsIgnoreCase(empRole))) {
                continue;
            }

            // Kiểm tra phòng đã tồn tại chưa
            InternalChatRoom room = roomRepository.findByEmployeeAccountId(emp.getAccountRole().getAccount().getId())
                    .orElseGet(() -> createRoomForEmployee(emp, office, managerEmployee));

            rooms.add(toRoomDto(room));
        }

        // Sắp xếp theo thời gian tin nhắn gần nhất
        rooms.sort((a, b) -> {
            if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) return 0;
            if (a.getLastMessageAt() == null) return 1;
            if (b.getLastMessageAt() == null) return -1;
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        });

        return rooms;
    }

    private InternalChatRoom createRoomForEmployee(Employee employee, Office office, Employee managerEmployee) {
        User employeeUser = employee.getUser();
        String employeeName = employeeUser != null ? employeeUser.getFullName() : "Nhân viên";
        String employeeRole = employee.getAccountRole() != null && employee.getAccountRole().getRole() != null
                ? employee.getAccountRole().getRole().getName() : "Unknown";

        InternalChatRoom room = new InternalChatRoom();
        room.setEmployeeAccountId(employee.getAccountRole().getAccount().getId());
        room.setEmployeeName(employeeName);
        room.setEmployeeRole(employeeRole);
        room.setManagerAccountId(managerEmployee.getAccountRole().getAccount().getId());
        room.setManagerName(managerEmployee.getUser() != null ? managerEmployee.getUser().getFullName() : "Quản lý");
        room.setOfficeId(office.getId());
        room.setOfficeName(office.getName());

        return roomRepository.save(room);
    }

    public List<InternalChatMessageDto> getMessages(Integer roomId) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        InternalChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_ROOM_NOT_FOUND));

        if (!canAccessRoom(room, accountId, roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        return messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(msg -> toMessageDto(msg, accountId))
                .collect(Collectors.toList());
    }

    @Transactional
    public InternalChatMessageDto sendMessage(Integer roomId, String messageText) {
        SendInternalChatMessageRequest request = new SendInternalChatMessageRequest();
        request.setMessage(messageText);
        return sendMessage(roomId, request);
    }

    @Transactional
    public InternalChatMessageDto sendMessage(Integer roomId, SendInternalChatMessageRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        InternalChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_ROOM_NOT_FOUND));

        if (!canAccessRoom(room, accountId, roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        User user = userRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_USER_NOT_FOUND));

        String senderName = user.getFullName();

        InternalChatMessage message = new InternalChatMessage();
        message.setRoomId(roomId);
        message.setSenderAccountId(accountId);
        message.setSenderName(senderName);
        message.setSenderRole(roleName);
        message.setMessage(request.getMessage());
        message.setMessageType("TEXT");
        message.setIsRead(false);

        message = messageRepository.save(message);

        roomRepository.updateLastMessage(roomId, request.getMessage(), LocalDateTime.now(), accountId);

        InternalChatMessageDto dto = toMessageDto(message, accountId);

        messagingTemplate.convertAndSend("/topic/internal-chat/" + roomId, dto);

        return dto;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public InternalChatMessageDto sendImageMessage(Integer roomId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_IMAGE_INVALID_TYPE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_IMAGE_INVALID_TYPE);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_IMAGE_SIZE_EXCEEDED);
        }

        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        InternalChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_ROOM_NOT_FOUND));

        if (!canAccessRoom(room, accountId, roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        // Upload to Cloudinary
        String imageUrl;
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "internal-chat",
                            "resource_type", "image"));
            imageUrl = uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_IMAGE_UPLOAD_FAILED);
        }

        User user = userRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_USER_NOT_FOUND));

        InternalChatMessage message = new InternalChatMessage();
        message.setRoomId(roomId);
        message.setSenderAccountId(accountId);
        message.setSenderName(user.getFullName());
        message.setSenderRole(roleName);
        message.setMessage("[Hình ảnh]");
        message.setMessageType("IMAGE");
        message.setImageUrl(imageUrl);
        message.setIsRead(false);

        message = messageRepository.save(message);

        roomRepository.updateLastMessage(roomId, "[Hình ảnh]", LocalDateTime.now(), accountId);

        InternalChatMessageDto dto = toMessageDto(message, accountId);

        messagingTemplate.convertAndSend("/topic/internal-chat/" + roomId, dto);

        return dto;
    }

    @Transactional
    public void markRoomAsRead(Integer roomId) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();

        InternalChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_ROOM_NOT_FOUND));

        String roleName = getCurrentRoleName();
        if (!canAccessRoom(room, accountId, roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        messageRepository.markMessagesAsRead(roomId, accountId);
    }

    public InternalChatRoomDto getRoomById(Integer roomId) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        String roleName = getCurrentRoleName();

        InternalChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(InternalChatErrorCode.INTERNAL_CHAT_ROOM_NOT_FOUND));

        if (!canAccessRoom(room, accountId, roleName)) {
            throw new AppException(InternalChatErrorCode.INTERNAL_CHAT_INVALID_ROLE);
        }

        return toRoomDto(room);
    }

    private boolean canAccessRoom(InternalChatRoom room, Integer accountId, String roleName) {
        if (ROLE_ADMIN.equalsIgnoreCase(roleName)) {
            return true;
        }

        if (room.getEmployeeAccountId().equals(accountId) || room.getManagerAccountId().equals(accountId)) {
            return true;
        }

        return false;
    }

    private String getCurrentRoleName() {
        var role = SecurityUtils.getAuthenticatedUserRole();
        return role != null ? role.getName() : "";
    }

    private InternalChatRoomDto toRoomDto(InternalChatRoom room) {
        long unreadCount = messageRepository.countUnreadByRoomIdAndSenderAccountIdNot(
                room.getId(), SecurityUtils.getAuthenticatedAccountId());

        String employeeAvatar = getUserAvatar(room.getEmployeeAccountId());
        String managerAvatar = getUserAvatar(room.getManagerAccountId());

        return InternalChatRoomDto.builder()
                .id(room.getId())
                .employeeAccountId(room.getEmployeeAccountId())
                .employeeName(room.getEmployeeName())
                .employeeRole(room.getEmployeeRole())
                .employeeAvatar(employeeAvatar)
                .managerAccountId(room.getManagerAccountId())
                .managerName(room.getManagerName())
                .managerAvatar(managerAvatar)
                .officeId(room.getOfficeId())
                .officeName(room.getOfficeName())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .lastSenderAccountId(room.getLastSenderAccountId())
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private String getUserAvatar(Integer accountId) {
        if (accountId == null) {
            return null;
        }
        return userRepository.findByAccountId(accountId)
                .map(User::getImages)
                .orElse(null);
    }

    private InternalChatMessageDto toMessageDto(InternalChatMessage message, Integer currentAccountId) {
        String senderAvatar = getUserAvatar(message.getSenderAccountId());

        return InternalChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderAccountId(message.getSenderAccountId())
                .senderName(message.getSenderName())
                .senderRole(message.getSenderRole())
                .senderAvatar(senderAvatar)
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .isMine(message.getSenderAccountId().equals(currentAccountId))
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
