package com.pulsepass.repository;

import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String userEmail);

    // FR-TKT-006: tickets de un usuario por email filtrando por estado
    List<Ticket> findByUserEmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(String userEmail, TicketStatus status);

    // FR-SRC-004: tickets cuyo evento es posterior a una fecha, en orden cronológico
    List<Ticket> findByEventEventDateAfterOrderByEventEventDateAsc(LocalDateTime date);

    @Query("SELECT t FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status= :status")
    List<Ticket> findPaidByEventCode(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = :status")
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);

    // PRD Seguridad §11.1: ¿el ticket es de este usuario? Lo usa AuthorizationService.
    // "UserId" navega ticket.user.id (la FK user_id); exists solo hace un SELECT 1, sin cargar el ticket
    boolean existsByTicketCodeAndUserId(String ticketCode, Long userId);
}
