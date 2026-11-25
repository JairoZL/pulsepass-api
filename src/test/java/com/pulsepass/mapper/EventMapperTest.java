package com.pulsepass.mapper;

import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.model.Artist;
import com.pulsepass.model.Event;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;
import com.pulsepass.model.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Test del código que MapStruct genera, sin Spring ni Docker. Se instancia EventMapperImpl a mano
// y se le inyecta ArtistMapperImpl: con componentModel = "spring" la dependencia es un campo
// @Autowired, por lo que Mappers.getMapper() la dejaría en null.
class EventMapperTest {

    private EventMapper mapper;
    private Event event;
    private LocalDateTime date;

    @BeforeEach
    void setUp() {
        mapper = new EventMapperImpl();
        ReflectionTestUtils.setField(mapper, "artistMapper", new ArtistMapperImpl());

        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3);
        date = LocalDateTime.now().plusDays(30);
        event = new Event("CMF-2026", "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                EventStatus.PUBLISHED, date, 18, venue);
        Artist artist = new Artist("Solar Beat", "Colombia", "Pop");
        ReflectionTestUtils.setField(artist, "id", 10L);
        event.addArtist(artist);
    }

    @Test
    @DisplayName("toResponse: venueCode, venueName y artists derivados NO salen null")
    void toResponse_mapsDerivedFields() {
        // ACT
        EventResponse response = mapper.toResponse(event);

        // ASSERT
        assertThat(response.venueCode()).isNotNull().isEqualTo("VEN-SMR-01");
        assertThat(response.venueName()).isNotNull().isEqualTo("Marina Convention Center");
        assertThat(response.artists()).hasSize(1);
        assertThat(response.artists().get(0).stageName()).isEqualTo("Solar Beat");
        assertThat(response.artists().get(0).id()).isEqualTo(10L);
        assertThat(response.eventCode()).isEqualTo("CMF-2026");
        assertThat(response.name()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(response.eventDate()).isEqualTo(date);
        assertThat(response.minimumAge()).isEqualTo(18);
    }

    @Test
    @DisplayName("toSummary: venueName derivado NO sale null")
    void toSummary_mapsVenueName() {
        // ACT
        EventSummaryResponse summary = mapper.toSummary(event);

        // ASSERT
        assertThat(summary.venueName()).isNotNull().isEqualTo("Marina Convention Center");
        assertThat(summary.eventCode()).isEqualTo("CMF-2026");
        assertThat(summary.status()).isEqualTo(EventStatus.PUBLISHED);
    }
}
