package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.InternalChatMessage;

@Repository
public interface InternalChatMessageRepository extends JpaRepository<InternalChatMessage, Integer> {

    List<InternalChatMessage> findByRoomIdOrderByCreatedAtAsc(Integer roomId);

    @Modifying
    @Query("UPDATE InternalChatMessage m SET m.isRead = true WHERE m.roomId = :roomId AND m.senderAccountId != :currentAccountId AND m.isRead = false")
    void markMessagesAsRead(@Param("roomId") Integer roomId, @Param("currentAccountId") Integer currentAccountId);

    @Query("SELECT COUNT(m) FROM InternalChatMessage m WHERE m.roomId = :roomId AND m.senderAccountId != :accountId AND m.isRead = false")
    long countUnreadByRoomIdAndSenderAccountIdNot(@Param("roomId") Integer roomId, @Param("accountId") Integer accountId);
}
