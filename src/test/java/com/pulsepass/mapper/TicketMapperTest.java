package com.pulsepass.mapper;

import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;
import com.pulsepass.model.User;
import com.pulsepass.model.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Test del código que MapStruct genera, sin Spring ni Docker (TicketMapper no depende de otros mappers).
class TicketMapperTest {

    private final TicketMapper mapper = new TicketMapperImpl();

    @Test
    @DisplayName("toResponse: userEmail, eventCode y eventName derivados NO salen null (PRD §34)")
    void toResponse_mapsDerivedFields() {
        // ARRANGE
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3);
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18, venue);
        User user = new User("andrea", "andrea@email.com");
        LocalDateTime purchaseDate = LocalDateTime.now();
        Ticket ticket = new Ticket("TKT-ABC123", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, purchaseDate, user, event);

        // ACT
        TicketResponse response = mapper.toResponse(ticket);

        // ASSERT
        assertThat(response.userEmail()).isNotNull().isEqualTo("andrea@email.com");
        assertThat(response.eventCode()).isNotNull().isEqualTo("CMF-2026");
        assertThat(response.eventName()).isNotNull().isEqualTo("Caribbean Music Fest 2026");
        assertThat(response.ticketCode()).isEqualTo("TKT-ABC123");
        assertThat(response.type()).isEqualTo(TicketType.VIP);
        assertThat(response.price()).isEqualByComparingTo("250000.00");
        assertThat(response.status()).isEqualTo(TicketStatus.PAID);
        assertThat(response.purchaseDate()).isEqualTo(purchaseDate);
    }
}
