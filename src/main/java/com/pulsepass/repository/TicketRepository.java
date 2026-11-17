package com.pulsepass.repository;

import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String userEmail);

    @Query("SELECT t FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status= :status")
    List<Ticket> findPaidByEventCode(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = :status")
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);
}
