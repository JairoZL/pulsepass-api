package com.pulsepass.repository;

import com.pulsepass.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // Se usa para obtener birthDate del usuario (BR-TICKET-006) y para guardar el perfil al registrar (BR-USER-004)
    Optional<UserProfile> findByUserId(Long userId);
}
