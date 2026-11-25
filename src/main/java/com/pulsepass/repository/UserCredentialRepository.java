package com.pulsepass.repository;

import com.pulsepass.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

// PRD Seguridad §14.5: acceso mínimo a credenciales.
// No necesita métodos propios: la PK es el id del usuario, así que
// findById(userId) ya devuelve la credencial de ese usuario.
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
