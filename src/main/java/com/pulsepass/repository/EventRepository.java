package com.pulsepass.repository;

import com.pulsepass.model.Event;
import com.pulsepass.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // Eventos de un venue por venue.code (Query Method navegando la relación)
    List<Event> findByVenueCodeOrderByEventDateAsc(String venueCode);

    // FR-SRC-001 / FR-ART-004: DISTINCT para no devolver duplicados al navegar la N:M
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a WHERE a.stageName = :stageName")
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos de una ciudad en los que participa un artista
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city AND a.stageName = :stageName
            ORDER BY e.eventDate ASC
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                             @Param("stageName") String stageName);

    // FR-SRC-003: eventos recomendados (publicados, posteriores a una fecha, en una ciudad,
    // con artista cuyo nombre contenga el texto; case-insensitive para el artista)
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = com.pulsepass.model.EventStatus.PUBLISHED
              AND e.eventDate > :fromDate
              AND v.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommended(@Param("fromDate") LocalDateTime fromDate,
                                @Param("city") String city,
                                @Param("artistText") String artistText);
}
