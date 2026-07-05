package com.logistics.service.chat;

import com.logistics.dto.chat.InternalChatMessageDto;
import com.logistics.dto.chat.InternalChatRoomDto;
import com.logistics.request.chat.SendInternalChatMessageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InternalChatService {
    InternalChatRoomDto getOrCreateMyRoom();
    List<InternalChatRoomDto> getOfficeRooms();
    List<InternalChatMessageDto> getMessages(Integer roomId);
    InternalChatMessageDto sendMessage(Integer roomId, String messageText);
    InternalChatMessageDto sendMessage(Integer roomId, SendInternalChatMessageRequest request);
    InternalChatMessageDto sendImageMessage(Integer roomId, MultipartFile file);
    void markRoomAsRead(Integer roomId);
    InternalChatRoomDto getRoomById(Integer roomId);
}
