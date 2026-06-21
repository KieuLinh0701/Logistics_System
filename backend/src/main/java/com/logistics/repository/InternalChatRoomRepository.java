package com.logistics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.InternalChatRoom;

@Repository
public interface InternalChatRoomRepository extends JpaRepository<InternalChatRoom, Integer> {

    Optional<InternalChatRoom> findByEmployeeAccountId(Integer employeeAccountId);

    List<InternalChatRoom> findByOfficeIdOrderByUpdatedAtDesc(Integer officeId);

    List<InternalChatRoom> findByManagerAccountIdOrderByUpdatedAtDesc(Integer managerAccountId);

    @Query("SELECT r FROM InternalChatRoom r WHERE r.officeId = :officeId ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<InternalChatRoom> findOfficeRoomsOrderByLastMessage(@Param("officeId") Integer officeId);

    @Modifying
    @Query("UPDATE InternalChatRoom r SET r.lastMessage = :message, r.lastMessageAt = :lastMessageAt, " +
           "r.lastSenderAccountId = :senderAccountId WHERE r.id = :roomId")
    void updateLastMessage(@Param("roomId") Integer roomId,
                           @Param("message") String message,
                           @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt,
                           @Param("senderAccountId") Integer senderAccountId);
}
