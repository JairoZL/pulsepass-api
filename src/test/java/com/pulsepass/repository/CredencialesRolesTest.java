package com.pulsepass.repository;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.model.Role;
import com.pulsepass.model.User;
import com.pulsepass.model.UserCredential;
import com.pulsepass.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PRD Seguridad §9 y §17.3: migración V4 (user_credentials y user_roles) contra PostgreSQL real.
 * Las pruebas de mapeo usan JPA; las de restricciones usan SQL directo con JdbcTemplate,
 * porque lo que se quiere probar es que la BASE rechaza el dato, no que JPA lo evite.
 * @DataJpaTest revierte la transacción al terminar cada prueba.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CredencialesRolesTest {

    // Hash con formato BCrypt (60 caracteres). Es un valor ficticio solo para pruebas (§17.3)
    private static final String HASH_FICTICIO =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private User nuevoUsuario(String username) {
        return em.persist(new User(username, username + "@pulsepass.com"));
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @Test
    void v4_dadoUnUsuarioConCredencial_cuandoSeBuscaPorSuId_entoncesSeRecuperaElHash() {
        // Dado: un usuario guardado y su credencial
        User andrea = nuevoUsuario("andrea");
        em.persist(new UserCredential(andrea, HASH_FICTICIO));
        Long userId = andrea.getId();
        sincronizar();

        // Cuando: se busca la credencial por el id del usuario (PK compartida con @MapsId)
        Optional<UserCredential> credencial = userCredentialRepository.findById(userId);

        // Entonces: existe, guarda el hash tal cual y registra la fecha del cambio
        assertThat(credencial).isPresent();
        assertThat(credencial.get().getPasswordHash()).isEqualTo(HASH_FICTICIO);
        assertThat(credencial.get().getPasswordChangedAt()).isNotNull();
        assertThat(credencial.get().getUser().getId()).isEqualTo(userId);
    }

    @Test
    void v4_dadoUnUsuarioConDosRoles_cuandoSeConsultanSusRoles_entoncesDevuelveAmbos() {
        // Dado: un usuario con roles USER y STAFF
        User laura = nuevoUsuario("laura");
        em.persist(new UserRole(laura, Role.USER));
        em.persist(new UserRole(laura, Role.STAFF));
        Long userId = laura.getId();
        sincronizar();

        // Cuando: se consultan sus roles
        List<UserRole> roles = userRoleRepository.findByUserId(userId);

        // Entonces: aparecen los dos, leídos desde la base
        assertThat(roles).extracting(UserRole::getRole)
                .containsExactlyInAnyOrder(Role.USER, Role.STAFF);
    }

    @Test
    void v4_dadosDosUsuarios_cuandoSeConsultanLosRolesDeUno_entoncesNoMezclaLosDelOtro() {
        // Dado: Andrea es USER y Miguel es ADMIN
        User andrea = nuevoUsuario("andrea");
        User miguel = nuevoUsuario("miguel");
        em.persist(new UserRole(andrea, Role.USER));
        em.persist(new UserRole(miguel, Role.ADMIN));
        Long andreaId = andrea.getId();
        sincronizar();

        // Cuando: se consultan los roles de Andrea
        List<UserRole> roles = userRoleRepository.findByUserId(andreaId);

        // Entonces: solo tiene USER
        assertThat(roles).extracting(UserRole::getRole).containsExactly(Role.USER);
    }

    @Test
    void v4_dadoUnRolYaAsignado_cuandoSeInsertaOtraVez_entoncesLaPkCompuestaLoRechaza() {
        // Dado: un usuario que ya tiene ROLE_USER
        User andrea = nuevoUsuario("andrea");
        em.flush();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'USER')", andrea.getId());

        // Cuando / Entonces: repetir el mismo rol viola pk_user_roles
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, 'USER')", andrea.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v4_dadoUnRolFueraDelEnum_cuandoSeInserta_entoncesElCheckLoRechaza() {
        // Dado: un usuario existente
        User andrea = nuevoUsuario("andrea");
        em.flush();

        // Cuando / Entonces: un rol que no es USER, STAFF ni ADMIN viola ck_user_roles_role
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, 'ROOT')", andrea.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v4_dadoUnUsuarioConCredencial_cuandoSeInsertaUnaSegunda_entoncesLaRelacionUnoAUnoLoImpide() {
        // Dado: un usuario que ya tiene credencial
        User andrea = nuevoUsuario("andrea");
        em.flush();
        jdbcTemplate.update("INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)",
                andrea.getId(), HASH_FICTICIO);

        // Cuando / Entonces: una segunda credencial para el mismo usuario viola pk_user_credentials
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)",
                andrea.getId(), HASH_FICTICIO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v4_dadoUnIdDeUsuarioInexistente_cuandoSeLeCreaCredencial_entoncesLaFkLoRechaza() {
        // Dado: un id que no existe en users
        long idInexistente = 999_999L;

        // Cuando / Entonces: la credencial huérfana viola fk_user_credentials_user
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)",
                idInexistente, HASH_FICTICIO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v4_dadaUnaCredencial_cuandoSeOmiteElHash_entoncesNotNullLoRechaza() {
        // Dado: un usuario existente
        User andrea = nuevoUsuario("andrea");
        em.flush();

        // Cuando / Entonces: password_hash es NOT NULL; no puede existir credencial sin hash
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, NULL)", andrea.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
