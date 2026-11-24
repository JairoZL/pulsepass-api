package com.pulsepass.repository;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.model.Artist;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;
import com.pulsepass.model.User;
import com.pulsepass.model.UserProfile;
import com.pulsepass.model.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QT-003 a QT-006: relaciones del modelo contra PostgreSQL real.
 * Cada prueba hace flush + clear para releer desde la base y no desde la caché de primer nivel.
 * @DataJpaTest revierte la transacción al terminar cada prueba.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RelacionesTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ArtistRepository artistRepository;

    private Venue nuevoVenue(String code) {
        return em.persist(new Venue(code, "Venue " + code, "Santa Marta", "Calle 1 # 2-3", 500));
    }

    private Event nuevoEvento(String code, Venue venue) {
        return em.persist(new Event(code, "Evento " + code, "Descripción " + code, EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.of(2027, 1, 15, 20, 0), 18, venue));
    }

    private User nuevoUsuario(String username) {
        return em.persist(new User(username, username + "@pulsepass.com"));
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }




    @Test
    void qt003_dadoUnVenueConDosEventos_cuandoSeRelee_entoncesCadaEventoApuntaAlMismoVenue() {
        // Dado: un venue con dos eventos
        Venue venue = nuevoVenue("VEN-001");
        nuevoEvento("EVT-001", venue);
        nuevoEvento("EVT-002", venue);
        Long venueId = venue.getId();
        sincronizar();

        // Cuando: se leen los eventos desde la base
        Event e1 = eventRepository.findByEventCode("EVT-001").orElseThrow();
        Event e2 = eventRepository.findByEventCode("EVT-002").orElseThrow();

        // Entonces: ambos referencian el mismo venue (1:N)
        assertThat(e1.getVenue().getId()).isEqualTo(venueId);
        assertThat(e2.getVenue().getId()).isEqualTo(venueId);
        assertThat(eventRepository.findByVenueCodeOrderByEventDateAsc("VEN-001")).hasSize(2);
    }

    @Test
    void qt004_dadoUnUsuarioConPerfil_cuandoSeRelee_entoncesElPerfilApuntaAlUsuario() {
        // Dado: un usuario con su perfil
        User user = nuevoUsuario("jairo");
        em.persist(new UserProfile(user, "Jairo", "Zapateiro", "3001234567", "Santa Marta",
                LocalDate.of(2000, 5, 20)));
        Long userId = user.getId();
        sincronizar();

        // Cuando: se lee el perfil desde la base
        UserProfile perfil = em.getEntityManager()
                .createQuery("SELECT p FROM UserProfile p WHERE p.user.id = :id", UserProfile.class)
                .setParameter("id", userId)
                .getSingleResult();

        // Entonces: el perfil pertenece a ese usuario (1:1)
        assertThat(perfil.getUser().getId()).isEqualTo(userId);
        assertThat(perfil.getFirstName()).isEqualTo("Jairo");
        assertThat(userRepository.findByEmailIgnoreCase("JAIRO@pulsepass.com")).isPresent();
    }

    @Test
    void qt005_dadoUnEventoConDosArtistas_cuandoSeRelee_entoncesTieneAmbosArtistas() {
        // Dado: un evento al que se le asocian dos artistas iniciales (V2)
        Artist solarBeat = artistRepository.findByStageNameIgnoreCase("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository.findByStageNameIgnoreCase("Neon Waves").orElseThrow();
        Event evento = nuevoEvento("EVT-ART", nuevoVenue("VEN-ART"));
        evento.addArtist(solarBeat);
        evento.addArtist(neonWaves);
        sincronizar();

        // Cuando: se relee el evento desde la base
        Event releido = eventRepository.findByEventCode("EVT-ART").orElseThrow();

        // Entonces: tiene ambos artistas y aparece en la consulta JPQL de cada uno (N:M)
        assertThat(releido.getArtists()).extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat", "Neon Waves");
        assertThat(eventRepository.findByArtistStageName("Solar Beat"))
                .extracting(Event::getEventCode).contains("EVT-ART");
        assertThat(eventRepository.findByArtistStageName("Neon Waves"))
                .extracting(Event::getEventCode).contains("EVT-ART");
    }

    @Test
    void qt006_dadoUnTicketDeUnUsuarioParaUnEvento_cuandoSeRelee_entoncesApuntaAAmbos() {
        // Dado: un ticket asociado a un usuario y a un evento
        User user = nuevoUsuario("comprador");
        Event evento = nuevoEvento("EVT-TKT", nuevoVenue("VEN-TKT"));
        em.persist(new Ticket("TKT-001", TicketType.GENERAL, new BigDecimal("50000.00"),
                TicketStatus.PAID, LocalDateTime.of(2026, 11, 1, 10, 0), user, evento));
        Long userId = user.getId();
        Long eventoId = evento.getId();
        sincronizar();

        // Cuando: se lee el ticket desde la base
        Ticket ticket = ticketRepository.findByTicketCode("TKT-001").orElseThrow();

        // Entonces: referencia al usuario y al evento correctos (Ticket -> User, Ticket -> Event)
        assertThat(ticket.getUser().getId()).isEqualTo(userId);
        assertThat(ticket.getEvent().getId()).isEqualTo(eventoId);
        assertThat(ticket.getPrice()).isEqualByComparingTo("50000.00");
        assertThat(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("comprador@pulsepass.com"))
                .hasSize(1);
    }
}
