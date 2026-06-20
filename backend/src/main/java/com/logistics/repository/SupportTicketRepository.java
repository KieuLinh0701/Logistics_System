package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.logistics.entity.SupportTicket;
import com.logistics.enums.SupportTicketStatus;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {
    List<SupportTicket> findByCreatedByAccountIdOrderByUpdatedAtDesc(Integer createdByAccountId);

    List<SupportTicket> findAllByOrderByUpdatedAtDesc();

    List<SupportTicket> findByAssignedToAccountId(Integer assignedToAccountId);

    List<SupportTicket> findByAssignedToAccountIdOrderByUpdatedAtDesc(Integer assignedToAccountId);

    long countByAssignedToAccountId(Integer assignedToAccountId);

    @Query("""
        SELECT t FROM SupportTicket t
        WHERE t.relatedType = 'ORDER' AND EXISTS (
            SELECT 1 FROM com.logistics.entity.Order o WHERE o.id = t.relatedId AND (o.fromOffice.id = :officeId OR o.toOffice.id = :officeId)
        )
        ORDER BY t.updatedAt DESC
    """)
    List<SupportTicket> findByOrderOfficeId(@Param("officeId") Integer officeId);

    List<SupportTicket> findByCreatedByAccountId(Integer createdByAccountId);

    List<SupportTicket> findByCreatedByAccountIdOrderByCreatedAtDesc(Integer createdByAccountId);

    List<SupportTicket> findByStatusOrderByUpdatedAtDesc(SupportTicketStatus status);

    List<SupportTicket> findByOfficeIdOrderByUpdatedAtDesc(Integer officeId);

    long countByStatus(SupportTicketStatus status);
}
