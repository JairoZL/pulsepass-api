package com.pulsepass.repository;

import com.pulsepass.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    List<Artist> findByActiveTrueOrderByStageNameAsc();


}
