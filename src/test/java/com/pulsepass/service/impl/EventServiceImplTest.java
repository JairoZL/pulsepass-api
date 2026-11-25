package com.pulsepass.service.impl;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.model.Artist;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Venue;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Servicios §42): Service real + Repository/Mapper mock.
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final String EVENT_CODE = "CMF-2026";
    private static final String VENUE_CODE = "VEN-SMR-01";
    private static final Long ARTIST_ID = 10L;

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private Venue venue;
    private Event event;
    private Artist artist;

    @BeforeEach
    void setUp() {
        venue = new Venue(VENUE_CODE, "Marina Convention Center", "Santa Marta", "Calle 1", 3);
        event = new Event(EVENT_CODE, "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(30), 18, venue);
        artist = new Artist("Solar Beat", "Colombia", "Pop");
        ReflectionTestUtils.setField(artist, "id", ARTIST_ID);
    }

    // ---------- findByCode / listados ----------

    @Test
    @DisplayName("TEST-EVENT-001: evento existente -> retorna DTO")
    void findByCode_existingEvent_returnsDto() {
        // ARRANGE
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.findByCode(EVENT_CODE);

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("TEST-EVENT-002: evento inexistente -> ResourceNotFoundException")
    void findByCode_missingEvent_throwsNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.findByCode(EVENT_CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + EVENT_CODE);
    }

    @Test
    @DisplayName("FR-SVC-005: findPublishedEvents consulta solo PUBLISHED y mapea a resumen")
    void findPublishedEvents_returnsSummaries() {
        // ARRANGE
        EventSummaryResponse summary = summary();
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = eventService.findPublishedEvents();

        // ASSERT
        assertThat(result).containsExactly(summary);
    }

    @Test
    @DisplayName("FR-SVC-008: findByArtist mapea los eventos del artista a resumen")
    void findByArtist_returnsSummaries() {
        // ARRANGE
        EventSummaryResponse summary = summary();
        when(eventRepository.findByArtistStageName("Solar Beat")).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = eventService.findByArtist("Solar Beat");

        // ASSERT
        assertThat(result).containsExactly(summary);
    }

    // ---------- create ----------

    @Test
    @DisplayName("TEST-EVENT-003 / BR-EVENT-005: crear evento válido -> save() ejecutado y estado DRAFT")
    void create_validRequest_savesDraftEvent() {
        // ARRANGE
        CreateEventRequest request = createRequest(LocalDateTime.now().plusDays(30), 18);
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse result = eventService.create(request);

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(saved.getEventCode()).isEqualTo(EVENT_CODE);
        assertThat(saved.getVenue()).isSameAs(venue);
        assertThat(saved.getMinimumAge()).isEqualTo(18);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("BR-EVENT-001: código repetido -> DuplicateResourceException")
    void create_duplicateCode_throwsDuplicate() {
        // ARRANGE
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(createRequest(LocalDateTime.now().plusDays(30), 18)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(EVENT_CODE);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-EVENT-004 / BR-EVENT-002: venue inexistente -> error y save() nunca ejecutado")
    void create_venueNotFound_throwsNotFoundAndNeverSaves() {
        // ARRANGE
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(createRequest(LocalDateTime.now().plusDays(30), 18)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: " + VENUE_CODE);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-EVENT-005 / BR-EVENT-003: venue inactivo -> BusinessRuleException")
    void create_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(venue, "active", false);
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(createRequest(LocalDateTime.now().plusDays(30), 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Venue is not active");
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-EVENT-006 / BR-EVENT-004: fecha pasada -> BusinessRuleException")
    void create_pastDate_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(createRequest(LocalDateTime.now().minusDays(1), 18)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-006: minimumAge negativo -> BusinessRuleException")
    void create_negativeMinimumAge_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(createRequest(LocalDateTime.now().plusDays(30), -1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Minimum age");
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- publish ----------

    @Test
    @DisplayName("TEST-EVENT-007 / BR-EVENT-007: publicar DRAFT válido -> PUBLISHED")
    void publish_validDraft_becomesPublished() {
        // ARRANGE
        EventResponse expected = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.publish(EVENT_CODE);

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result).isEqualTo(expected);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("TEST-EVENT-008 / BR-EVENT-007: publicar CANCELLED -> BusinessRuleException y no persiste")
    void publish_cancelledEvent_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-007: publicar un evento ya PUBLISHED -> BusinessRuleException")
    void publish_alreadyPublished_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-008: publicar con fecha ya pasada -> BusinessRuleException")
    void publish_pastDate_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(event, "eventDate", LocalDateTime.now().minusDays(1));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-009: publicar con venue inactivo -> BusinessRuleException")
    void publish_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        ReflectionTestUtils.setField(venue, "active", false);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Venue is not active");
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("publish: evento inexistente -> ResourceNotFoundException")
    void publish_missingEvent_throwsNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + EVENT_CODE);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- addArtist ----------

    @Test
    @DisplayName("FR-SVC-007: addArtist válido -> artista asociado y evento guardado")
    void addArtist_validRequest_associatesArtist() {
        // ARRANGE
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = eventService.addArtist(EVENT_CODE, ARTIST_ID);

        // ASSERT
        assertThat(event.getArtists()).containsExactly(artist);
        assertThat(result).isEqualTo(expected);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("BR-EVENT-010: artista ya asociado -> DuplicateResourceException")
    void addArtist_alreadyAssigned_throwsDuplicate() {
        // ARRANGE
        event.addArtist(artist);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already assigned");
        assertThat(event.getArtists()).hasSize(1);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-011: evento CANCELLED -> BusinessRuleException")
    void addArtist_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
        assertThat(event.getArtists()).isEmpty();
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("BR-EVENT-011: evento FINISHED -> BusinessRuleException")
    void addArtist_finishedEvent_throwsBusinessRule() {
        // ARRANGE
        event.setStatus(EventStatus.FINISHED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("FINISHED");
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("addArtist: evento inexistente -> ResourceNotFoundException")
    void addArtist_missingEvent_throwsNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + EVENT_CODE);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("addArtist: artista inexistente -> ResourceNotFoundException")
    void addArtist_missingArtist_throwsNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: " + ARTIST_ID);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- helpers ----------

    private CreateEventRequest createRequest(LocalDateTime date, Integer minimumAge) {
        return new CreateEventRequest(EVENT_CODE, "Caribbean Music Fest 2026", "Festival",
                EventCategory.MUSIC, date, minimumAge, VENUE_CODE);
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, EVENT_CODE, "Caribbean Music Fest 2026", "Festival",
                EventCategory.MUSIC, status, event.getEventDate(), 18, VENUE_CODE,
                "Marina Convention Center", List.of());
    }

    private EventSummaryResponse summary() {
        return new EventSummaryResponse(EVENT_CODE, "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, event.getEventDate(), "Marina Convention Center");
    }
}
