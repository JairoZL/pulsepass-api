package com.pulsepass.repository;

import com.pulsepass.model.UserRole;
import com.pulsepass.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// PRD Seguridad §12.1: los roles se leen de la base en cada request, no del token.
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    // Query Method: navega user.id (la FK user_id). Lo usará CustomUserDetailsService
    List<UserRole> findByUserId(Long userId);
}
