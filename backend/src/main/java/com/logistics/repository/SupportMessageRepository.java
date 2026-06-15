package com.logistics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.entity.SupportMessage;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Integer> {
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);

    Optional<SupportMessage> findTopByTicketIdOrderByCreatedAtDesc(Integer ticketId);

    int countByTicketId(Integer ticketId);
}
