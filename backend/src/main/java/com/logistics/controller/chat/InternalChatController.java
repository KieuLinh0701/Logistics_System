package com.logistics.controller.chat;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.chat.InternalChatMessageDto;
import com.logistics.dto.chat.InternalChatRoomDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.chat.SendInternalChatMessageRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.chat.InternalChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/internal-chat")
@RequiredArgsConstructor
public class InternalChatController {

    private final InternalChatService internalChatService;

    @GetMapping("/my-room")
    public ResponseEntity<ApiResponse<InternalChatRoomDto>> getMyRoom() {
        InternalChatRoomDto room = internalChatService.getOrCreateMyRoom();
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<InternalChatRoomDto>>> getRooms() {
        List<InternalChatRoomDto> rooms = internalChatService.getOfficeRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<InternalChatRoomDto>> getRoom(@PathVariable Integer roomId) {
        InternalChatRoomDto room = internalChatService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<InternalChatMessageDto>>> getMessages(@PathVariable Integer roomId) {
        List<InternalChatMessageDto> messages = internalChatService.getMessages(roomId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/rooms/{roomId}/messages")
    @Audit(
            entity = EntityType.INTERNAL_CHAT_MESSAGE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.INTERNAL_CHAT_MESSAGE_CREATE,
            params = {"roomId"}
    )
    public ResponseEntity<ApiResponse<InternalChatMessageDto>> sendMessage(
            @PathVariable Integer roomId,
            @Valid @RequestBody SendInternalChatMessageRequest request) {
        InternalChatMessageDto message = internalChatService.sendMessage(roomId, request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/rooms/{roomId}/read")
    @Audit(
            entity = EntityType.INTERNAL_CHAT_MESSAGE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.INTERNAL_CHAT_MESSAGE_MARK_READ,
            params = {"roomId"}
    )
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Integer roomId) {
        internalChatService.markRoomAsRead(roomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/rooms/{roomId}/messages/image")
    @Audit(
            entity = EntityType.INTERNAL_CHAT_MESSAGE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.INTERNAL_CHAT_MESSAGE_CREATE,
            params = {"roomId"}
    )
    public ResponseEntity<ApiResponse<InternalChatMessageDto>> uploadImage(
            @PathVariable Integer roomId,
            @RequestParam("file") MultipartFile file) {
        InternalChatMessageDto message = internalChatService.sendImageMessage(roomId, file);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
