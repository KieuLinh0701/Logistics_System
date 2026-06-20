package com.logistics.repository;

import com.logistics.entity.SupportMessage;
import com.logistics.enums.SupportMessageSenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Integer> {
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);

    Optional<SupportMessage> findTopByTicketIdOrderByCreatedAtDesc(Integer ticketId);

    int countByTicketId(Integer ticketId);

    Optional<SupportMessage> findTopByTicketIdAndSenderTypeInOrderByCreatedAtDesc(Integer ticketId, List<SupportMessageSenderType> senderTypes);
}
