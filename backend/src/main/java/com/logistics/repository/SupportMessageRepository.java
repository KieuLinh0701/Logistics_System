package com.logistics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.SupportMessage;
import com.logistics.enums.SupportMessageSenderType;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Integer> {
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);

    Optional<SupportMessage> findTopByTicketIdOrderByCreatedAtDesc(Integer ticketId);

    int countByTicketId(Integer ticketId);

    Optional<SupportMessage> findTopByTicketIdAndSenderTypeInOrderByCreatedAtDesc(Integer ticketId, List<SupportMessageSenderType> senderTypes);

    List<SupportMessage> findByTicketIdAndSenderAccountIdNotAndIsReadFalse(Integer ticketId, Integer senderAccountId);

    long countByTicketIdAndSenderAccountIdNotAndIsReadFalse(Integer ticketId, Integer senderAccountId);

    @Modifying
    @Query("UPDATE SupportMessage m SET m.isRead = true WHERE m.ticketId = :ticketId AND m.senderAccountId != :accountId AND m.isRead = false")
    int markMessagesAsRead(@Param("ticketId") Integer ticketId, @Param("accountId") Integer accountId);
}
