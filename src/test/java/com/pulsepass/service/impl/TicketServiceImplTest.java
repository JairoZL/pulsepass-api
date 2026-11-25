package com.pulsepass.service.impl;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;
import com.pulsepass.model.User;
import com.pulsepass.model.UserProfile;
import com.pulsepass.model.Venue;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Servicios §42): Service real + Repository/Mapper/Calculator mock.
// Sin @SpringBootTest, sin PostgreSQL.
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String EMAIL = "andrea@email.com";
    private static final String EVENT_CODE = "CMF-2026";
    private static final String TICKET_CODE = "TKT-ABC123";

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketPriceCalculator priceCalculator;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private User user;
    private Venue venue;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User("andrea", EMAIL);
        ReflectionTestUtils.setField(user, "id", 1L);

        // Escenario del PRD §47: venue de capacidad 3
        venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3);
        event = new Event(EVENT_CODE, "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18, venue);
    }

    // ---------- purchase ----------

    @Test
    @DisplayName("TEST-TICKET-001: compra válida -> ticket PAID")
    void purchase_validRequest_savesPaidTicket() {
        // ARRANGE
        PurchaseTicketRequest request = new PurchaseTicketRequest(EMAIL, EVENT_CODE, TicketType.GENERAL);
        BigDecimal price = new BigDecimal("120000.00");
        TicketResponse expected = response(TicketStatus.PAID);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithAge(25)));
        when(ticketRepository.countByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.GENERAL)).thenReturn(price);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.purchase(request);

        // ASSERT
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getPrice()).isEqualByComparingTo(price);
        assertThat(saved.getType()).isEqualTo(TicketType.GENERAL);
        assertThat(saved.getTicketCode()).matches("TKT-[0-9A-F]{6}");
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEvent()).isSameAs(event);
        assertThat(result).isEqualTo(expected);
        // Quedan 2 cupos: el evento NO pasa a SOLD_OUT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-TICKET-002: usuario inexistente -> ResourceNotFoundException")
    void purchase_userNotFound_throwsNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + EMAIL);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TEST-TICKET-003: usuario inactivo -> BusinessRuleException")
    void purchase_inactiveUser_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(user, "active", false);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("User is not active");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-003: evento inexistente -> ResourceNotFoundException")
    void purchase_eventNotFound_throwsNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + EVENT_CODE);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TEST-TICKET-004: evento DRAFT -> BusinessRuleException")
    void purchase_draftEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.DRAFT);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TEST-TICKET-005: evento CANCELLED -> BusinessRuleException")
    void purchase_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.CANCELLED);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-005: evento que ya ocurrió -> BusinessRuleException")
    void purchase_pastEvent_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(event, "eventDate", LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already taken place");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TEST-TICKET-006: usuario menor de edad -> BusinessRuleException")
    void purchase_underage_throwsBusinessRule() {
        // ARRANGE: 16 años en la fecha del evento, mínimo 18
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithAge(16)));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-006: minimumAge = 0 no consulta el perfil ni valida edad")
    void purchase_noAgeRestriction_skipsAgeCheck() {
        // ARRANGE
        ReflectionTestUtils.setField(event, "minimumAge", 0);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.GENERAL)).thenReturn(new BigDecimal("120000.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(TicketStatus.PAID));

        // ACT
        ticketService.purchase(request());

        // ASSERT
        verify(userProfileRepository, never()).findByUserId(any());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TEST-TICKET-007: evento sin capacidad -> BusinessRuleException")
    void purchase_noCapacity_throwsBusinessRule() {
        // ARRANGE: 3 pagados, capacidad 3
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithAge(25)));
        when(ticketRepository.countByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(3L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no capacity");
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-TICKET-008: último ticket disponible -> guarda ticket y evento pasa a SOLD_OUT")
    void purchase_lastTicket_marksEventSoldOut() {
        // ARRANGE: 2 pagados, capacidad 3 -> esta compra llena el aforo
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profileWithAge(25)));
        when(ticketRepository.countByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(2L);
        when(priceCalculator.calculate(TicketType.GENERAL)).thenReturn(new BigDecimal("120000.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(TicketStatus.PAID));

        // ACT
        ticketService.purchase(request());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
    }

    // ---------- cancel ----------

    @Test
    @DisplayName("TEST-TICKET-009: cancelar ticket PAID -> CANCELLED")
    void cancel_paidTicket_becomesCancelled() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID);
        TicketResponse expected = response(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.cancel(TICKET_CODE);

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(result).isEqualTo(expected);
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("TEST-TICKET-010: cancelar ticket USED -> BusinessRuleException")
    void cancel_usedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.USED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-012: cancelar después de la fecha del evento -> BusinessRuleException")
    void cancel_afterEventDate_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(event, "eventDate", LocalDateTime.now().minusDays(1));
        Ticket ticket = ticket(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the event date");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- markAsUsed ----------

    @Test
    @DisplayName("TEST-TICKET-011: marcar PAID como usado -> USED")
    void markAsUsed_paidTicket_becomesUsed() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID);
        TicketResponse expected = response(TicketStatus.USED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = ticketService.markAsUsed(TICKET_CODE);

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(result).isEqualTo(expected);
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("TEST-TICKET-012: usar ticket CANCELLED -> BusinessRuleException")
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cancelled ticket cannot be used");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-011: cancelar ticket CANCELLED -> BusinessRuleException")
    void cancel_cancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-013: marcar como usado un ticket USED -> BusinessRuleException")
    void markAsUsed_usedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.USED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID tickets")
                .hasMessageContaining("USED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("BR-TICKET-013: marcar como usado un ticket RESERVED -> BusinessRuleException")
    void markAsUsed_reservedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.RESERVED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID tickets")
                .hasMessageContaining("RESERVED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESERVED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- helpers ----------

    private PurchaseTicketRequest request() {
        return new PurchaseTicketRequest(EMAIL, EVENT_CODE, TicketType.GENERAL);
    }

    // Perfil cuyo dueño tiene `age` años exactos en la fecha del evento
    private UserProfile profileWithAge(int age) {
        return new UserProfile(user, "Andrea", "Perez", null, null,
                event.getEventDate().toLocalDate().minusYears(age));
    }

    private Ticket ticket(TicketStatus status) {
        return new Ticket(TICKET_CODE, TicketType.GENERAL, new BigDecimal("120000.00"), status,
                LocalDateTime.now().minusDays(1), user, event);
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(1L, TICKET_CODE, TicketType.GENERAL, new BigDecimal("120000.00"),
                status, LocalDateTime.now(), EMAIL, EVENT_CODE, "Caribbean Music Fest 2026");
    }
}
