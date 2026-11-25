package com.pulsepass.repository;

import com.pulsepass.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QT-001 y QT-002: el esquema lo crea Flyway y Hibernate solo lo valida.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EsquemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManager entityManager;

    @Test
    void qt001_dadaUnaBaseVacia_cuandoArrancaFlyway_entoncesAplicaV1aV4() {
        // Dado: contenedor PostgreSQL nuevo (base vacía) y Flyway activo
        // Cuando: se consulta el historial de migraciones
        List<String> versiones = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank",
                String.class);

        // Entonces: están aplicadas V1 a V4, en orden (V4: credenciales y roles, PRD Seguridad §9.2)
        assertThat(versiones).containsExactly("1", "2", "3", "4");
    }

    @Test
    void qt001_dadaLaMigracionV2_cuandoSeConsultanLosArtistas_entoncesExistenLosCincoIniciales() {
        // Dado: V2 insertó los artistas iniciales
        // Cuando: se cuentan los artistas
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM artists", Integer.class);

        // Entonces: hay cinco
        assertThat(total).isEqualTo(5);
    }

    @Test
    void qt001_dadaLaMigracionV3_cuandoSeConsultaLaTablaEvents_entoncesExisteStreamingUrlOpcional() {
        // Dado: V3 agregó streaming_url a events (FR-EVT-006)
        // Cuando: se lee la metadata de la columna
        String nulable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'events' AND column_name = 'streaming_url'",
                String.class);

        // Entonces: la columna existe y admite NULL
        assertThat(nulable).isEqualTo("YES");
    }

    @Test
    void qt002_dadaLaConfiguracion_cuandoArrancaElContexto_entoncesHibernateSoloValidaElEsquema() {
        // Dado: el contexto JPA ya arrancó contra el esquema creado por Flyway
        // Cuando: se lee la propiedad ddl-auto
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

        // Entonces: es validate y el EntityManager está operativo
        //           (si el esquema no coincidiera con las entidades, el contexto no habría arrancado)
        assertThat(ddlAuto).isEqualTo("validate");
        assertThat(entityManager.isOpen()).isTrue();
    }
}
