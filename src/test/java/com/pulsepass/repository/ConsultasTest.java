package com.pulsepass.repository;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.model.Artist;
import com.pulsepass.model.Category;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Ticket;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;
import com.pulsepass.model.User;
import com.pulsepass.model.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QT-007 (Query Methods), QT-008 (JPQL con JOIN y COUNT) y QT-009 (UNIQUE con saveAndFlush),
 * más las búsquedas FR-SRC-001..004. PostgreSQL real vía Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ConsultasTest {

    private static final LocalDateTime FECHA_BASE = LocalDateTime.of(2027, 1, 15, 20, 0);

    @Autowired
    private TestEntityManager em;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private ArtistRepository artistRepository;

    private Venue nuevoVenue(String code, String ciudad) {
        return em.persist(new Venue(code, "Venue " + code, ciudad, "Calle 1 # 2-3", 500));
    }

    private Event nuevoEvento(String code, Venue venue, EventStatus status, LocalDateTime fecha) {
        return em.persist(new Event(code, "Evento " + code, "Descripción " + code, Category.MUSIC,
                status, fecha, 18, venue));
    }

    private User nuevoUsuario(String username) {
        return em.persist(new User(username, username + "@pulsepass.com"));
    }

    private Ticket nuevoTicket(String code, TicketStatus status, LocalDateTime compra, User user, Event event) {
        return em.persist(new Ticket(code, TicketType.GENERAL, new BigDecimal("50000.00"),
                status, compra, user, event));
    }

    private Artist artista(String nombre) {
        return artistRepository.findByStageNameIgnoreCase(nombre).orElseThrow();
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    // ---------- QT-007: Query Methods ----------

    @Test
    void qt007_fr_ven_004_eventosDeUnVenue_soloDevuelveLosDeEseVenueOrdenadosPorFecha() {
        Venue a = nuevoVenue("VEN-A", "Santa Marta");
        Venue b = nuevoVenue("VEN-B", "Bogotá");
        nuevoEvento("EVT-A2", a, EventStatus.PUBLISHED, FECHA_BASE.plusDays(5));
        nuevoEvento("EVT-A1", a, EventStatus.PUBLISHED, FECHA_BASE);
        nuevoEvento("EVT-B1", b, EventStatus.PUBLISHED, FECHA_BASE);
        sincronizar();

        assertThat(eventRepository.findByVenueCodeOrderByEventDateAsc("VEN-A"))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-A1", "EVT-A2");
    }

    @Test
    void qt007_fr_evt_005_eventosPublicados_excluyeOtrosEstadosYOrdenaPorFecha() {
        Venue venue = nuevoVenue("VEN-E", "Santa Marta");
        nuevoEvento("EVT-E2", venue, EventStatus.PUBLISHED, FECHA_BASE.plusDays(2));
        nuevoEvento("EVT-E1", venue, EventStatus.PUBLISHED, FECHA_BASE);
        nuevoEvento("EVT-E3", venue, EventStatus.DRAFT, FECHA_BASE.minusDays(1));
        sincronizar();

        assertThat(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-E1", "EVT-E2");
    }

    @Test
    void qt007_fr_tkt_006_ticketsPorEmailYEstado_filtraPorEstadoYOrdenaPorCompraDesc() {
        User user = nuevoUsuario("ana");
        User otro = nuevoUsuario("beto");
        Event evento = nuevoEvento("EVT-T", nuevoVenue("VEN-T", "Santa Marta"), EventStatus.PUBLISHED, FECHA_BASE);
        nuevoTicket("TKT-1", TicketStatus.PAID, LocalDateTime.of(2026, 11, 1, 10, 0), user, evento);
        nuevoTicket("TKT-2", TicketStatus.PAID, LocalDateTime.of(2026, 11, 2, 10, 0), user, evento);
        nuevoTicket("TKT-3", TicketStatus.RESERVED, LocalDateTime.of(2026, 11, 3, 10, 0), user, evento);
        nuevoTicket("TKT-4", TicketStatus.PAID, LocalDateTime.of(2026, 11, 4, 10, 0), otro, evento);
        sincronizar();

        assertThat(ticketRepository.findByUserEmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(
                "ANA@pulsepass.com", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-2", "TKT-1");
    }

    // ---------- QT-008: JPQL con JOIN y COUNT ----------

    @Test
    void qt008_fr_tkt_007_ticketsPagadosPorEvento_excluyeOtrosEstadosYOtrosEventos() {
        User user = nuevoUsuario("carla");
        Venue venue = nuevoVenue("VEN-P", "Santa Marta");
        Event e1 = nuevoEvento("EVT-P1", venue, EventStatus.PUBLISHED, FECHA_BASE);
        Event e2 = nuevoEvento("EVT-P2", venue, EventStatus.PUBLISHED, FECHA_BASE);
        nuevoTicket("TKT-P1", TicketStatus.PAID, FECHA_BASE.minusDays(30), user, e1);
        nuevoTicket("TKT-P2", TicketStatus.CANCELLED, FECHA_BASE.minusDays(29), user, e1);
        nuevoTicket("TKT-P3", TicketStatus.PAID, FECHA_BASE.minusDays(28), user, e2);
        sincronizar();

        assertThat(ticketRepository.findPaidByEventCode("EVT-P1", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-P1");
    }

    @Test
    void qt008_fr_tkt_008_conteoDeVentas_cuentaSoloTicketsPagadosDelEvento() {
        User user = nuevoUsuario("diego");
        Venue venue = nuevoVenue("VEN-C", "Santa Marta");
        Event e1 = nuevoEvento("EVT-C1", venue, EventStatus.PUBLISHED, FECHA_BASE);
        Event e2 = nuevoEvento("EVT-C2", venue, EventStatus.PUBLISHED, FECHA_BASE);
        nuevoTicket("TKT-C1", TicketStatus.PAID, FECHA_BASE.minusDays(30), user, e1);
        nuevoTicket("TKT-C2", TicketStatus.PAID, FECHA_BASE.minusDays(29), user, e1);
        nuevoTicket("TKT-C3", TicketStatus.RESERVED, FECHA_BASE.minusDays(28), user, e1);
        nuevoTicket("TKT-C4", TicketStatus.PAID, FECHA_BASE.minusDays(27), user, e2);
        sincronizar();

        assertThat(ticketRepository.countByEventCodeAndStatus("EVT-C1", TicketStatus.PAID)).isEqualTo(2);
        assertThat(ticketRepository.countByEventCodeAndStatus("EVT-SIN-VENTAS", TicketStatus.PAID)).isZero();
    }

    @Test
    void qt008_fr_src_001_eventosPorArtista_noDuplicaCuandoElEventoTieneVariosArtistas() {
        Event evento = nuevoEvento("EVT-S1", nuevoVenue("VEN-S1", "Santa Marta"), EventStatus.PUBLISHED, FECHA_BASE);
        evento.addArtist(artista("Solar Beat"));
        evento.addArtist(artista("Neon Waves"));
        sincronizar();

        assertThat(eventRepository.findByArtistStageName("Solar Beat"))
                .extracting(Event::getEventCode)
                .containsOnlyOnce("EVT-S1");
    }

    // ---------- FR-SRC-002 / 003 / 004 ----------

    @Test
    void fr_src_002_eventosPorCiudadYArtista_filtraPorAmbosYOrdenaPorFecha() {
        Venue santaMarta = nuevoVenue("VEN-SM", "Santa Marta");
        Venue bogota = nuevoVenue("VEN-BG", "Bogotá");
        Event tarde = nuevoEvento("EVT-SM2", santaMarta, EventStatus.PUBLISHED, FECHA_BASE.plusDays(10));
        Event temprano = nuevoEvento("EVT-SM1", santaMarta, EventStatus.PUBLISHED, FECHA_BASE);
        Event otraCiudad = nuevoEvento("EVT-BG1", bogota, EventStatus.PUBLISHED, FECHA_BASE);
        Event otroArtista = nuevoEvento("EVT-SM3", santaMarta, EventStatus.PUBLISHED, FECHA_BASE);
        tarde.addArtist(artista("Solar Beat"));
        temprano.addArtist(artista("Solar Beat"));
        otraCiudad.addArtist(artista("Solar Beat"));
        otroArtista.addArtist(artista("Neon Waves"));
        sincronizar();

        assertThat(eventRepository.findByCityAndArtistStageName("Santa Marta", "Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-SM1", "EVT-SM2");
    }

    @Test
    void fr_src_003_eventosRecomendados_aplicaTodosLosFiltrosSinDuplicadosYCaseInsensitive() {
        Venue santaMarta = nuevoVenue("VEN-R1", "Santa Marta");
        Venue bogota = nuevoVenue("VEN-R2", "Bogotá");
        LocalDateTime desde = FECHA_BASE.minusDays(1);

        Event valido = nuevoEvento("EVT-R1", santaMarta, EventStatus.PUBLISHED, FECHA_BASE);
        Event validoTarde = nuevoEvento("EVT-R2", santaMarta, EventStatus.PUBLISHED, FECHA_BASE.plusDays(3));
        Event borrador = nuevoEvento("EVT-R3", santaMarta, EventStatus.DRAFT, FECHA_BASE);
        Event pasado = nuevoEvento("EVT-R4", santaMarta, EventStatus.PUBLISHED, FECHA_BASE.minusDays(10));
        Event otraCiudad = nuevoEvento("EVT-R5", bogota, EventStatus.PUBLISHED, FECHA_BASE);
        // Dos artistas que coinciden con el texto "o": el evento no debe repetirse (DISTINCT)
        valido.addArtist(artista("Solar Beat"));
        valido.addArtist(artista("Neon Waves"));
        validoTarde.addArtist(artista("Solar Beat"));
        borrador.addArtist(artista("Solar Beat"));
        pasado.addArtist(artista("Solar Beat"));
        otraCiudad.addArtist(artista("Solar Beat"));
        sincronizar();

        assertThat(eventRepository.findRecommended(desde, "Santa Marta", "SOLAR"))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-R1", "EVT-R2");
        assertThat(eventRepository.findRecommended(desde, "Santa Marta", "o"))
                .extracting(Event::getEventCode)
                .containsOnlyOnce("EVT-R1");
    }

    @Test
    void fr_src_004_ticketsDeEventosFuturos_excluyePasadosYOrdenaPorFechaDeEvento() {
        User user = nuevoUsuario("elena");
        Venue venue = nuevoVenue("VEN-F", "Santa Marta");
        Event lejano = nuevoEvento("EVT-F1", venue, EventStatus.PUBLISHED, FECHA_BASE.plusDays(30));
        Event cercano = nuevoEvento("EVT-F2", venue, EventStatus.PUBLISHED, FECHA_BASE);
        Event pasado = nuevoEvento("EVT-F3", venue, EventStatus.FINISHED, FECHA_BASE.minusDays(60));
        nuevoTicket("TKT-F1", TicketStatus.PAID, FECHA_BASE.minusDays(40), user, lejano);
        nuevoTicket("TKT-F2", TicketStatus.PAID, FECHA_BASE.minusDays(39), user, cercano);
        nuevoTicket("TKT-F3", TicketStatus.PAID, FECHA_BASE.minusDays(70), user, pasado);
        sincronizar();

        assertThat(ticketRepository.findByEventEventDateAfterOrderByEventEventDateAsc(FECHA_BASE.minusDays(1)))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TKT-F2", "TKT-F1");
    }

    // ---------- QT-009: UNIQUE con saveAndFlush ----------

    @Test
    void qt009_fr_ven_002_venueConCodigoDuplicado_esRechazadoPorPostgreSQL() {
        venueRepository.saveAndFlush(new Venue("VEN-DUP", "Uno", "Santa Marta", "Calle 1", 100));

        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue("VEN-DUP", "Dos", "Bogotá", "Calle 2", 200)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
