package com.pulsepass.repository;

import com.pulsepass.model.Event;
import com.pulsepass.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    @Query("SELECT e FROM Event e JOIN e.artists a WHERE a.stageName=:stageName")
    List<Event> findByArtistStageName(@Param("stageName") String stageName);
}
