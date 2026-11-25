package com.pulsepass.service.impl;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.User;
import com.pulsepass.model.UserProfile;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EventRepository eventRepository;
    private final TicketPriceCalculator priceCalculator;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             UserProfileRepository userProfileRepository,
                             EventRepository eventRepository,
                             TicketPriceCalculator priceCalculator,
                             TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.eventRepository = eventRepository;
        this.priceCalculator = priceCalculator;
        this.ticketMapper = ticketMapper;
    }

    // PRD Servicios §24 y §39: toda la compra es una sola transacción.
    // Si cualquier paso lanza excepción, se hace ROLLBACK de todo (ticket y SOLD_OUT incluidos).
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: el usuario debe existir
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        // BR-TICKET-002: un usuario inactivo no puede comprar
        if (!user.getActive()) {
            throw new BusinessRuleException("User is not active: " + request.userEmail());
        }

        // BR-TICKET-003: el evento debe existir
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));
        // BR-TICKET-004: solo se compra para eventos PUBLISHED (no DRAFT, SOLD_OUT, CANCELLED ni FINISHED)
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Event is not available for purchase. Current status: " + event.getStatus());
        }
        // BR-TICKET-005: no se compra para un evento que ya ocurrió
        LocalDateTime now = LocalDateTime.now();
        if (!event.getEventDate().isAfter(now)) {
            throw new BusinessRuleException("Event has already taken place: " + request.eventCode());
        }

        // BR-TICKET-006: la edad se valida solo si el evento tiene restricción (minimumAge > 0)
        if (event.getMinimumAge() > 0) {
            UserProfile profile = userProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User profile not found: " + request.userEmail()));
            // La edad se calcula en la fecha del evento, no en la fecha de hoy
            int age = Period.between(profile.getBirthDate(), event.getEventDate().toLocalDate()).getYears();
            if (age < event.getMinimumAge()) {
                throw new BusinessRuleException(
                        "User does not meet minimum age: " + event.getMinimumAge());
            }
        }

        // BR-TICKET-007: si los tickets pagados ya igualan la capacidad del venue, no hay cupo
        long paidTickets = ticketRepository.countByEventCodeAndStatus(request.eventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity available: " + request.eventCode());
        }

        // BR-TICKET-009: el precio lo calcula el Service (el request no lo trae)
        BigDecimal price = priceCalculator.calculate(request.type());
        // PRD §27: en esta versión, una compra válida nace PAID
        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                now,
                user,
                event);
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra llena el aforo, el evento pasa a SOLD_OUT en la misma transacción
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findPaidByEventCode(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        // BR-TICKET-010: solo se cancela un ticket PAID.
        // BR-TICKET-011: por lo tanto USED y CANCELLED quedan rechazados por la misma condición.
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }
        // BR-TICKET-012: no se cancela después de la fecha del evento
        if (LocalDateTime.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        // BR-TICKET-014: un ticket CANCELLED nunca puede utilizarse (mensaje propio)
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled ticket cannot be used: " + ticketCode);
        }
        // BR-TICKET-013: solo un ticket PAID puede marcarse como usado
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // "TKT-" + 6 caracteres hexadecimales en mayúscula tomados de un UUID aleatorio.
    // Longitud 10, dentro del límite de 30 de tickets.ticket_code; el UNIQUE de la base
    // protege ante una colisión (poco probable, pero posible con solo 6 caracteres).
    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
