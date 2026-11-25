package com.pulsepass.security;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.model.Role;
import com.pulsepass.model.User;
import com.pulsepass.model.UserRole;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// PRD Seguridad §9.2, §23 y NFR-SEC-009: cuentas STAFF y ADMIN para la demo.
// El registro público SOLO crea USER, así que los roles privilegiados salen de aquí:
//   - Solo corre con el perfil "dev" (@Profile): en cualquier otro perfil no existe este bean.
//   - Las contraseñas vienen de variables de entorno (.env), nunca del código.
//   - Si la variable está vacía, esa cuenta no se crea (no hay contraseña por defecto).
//   - Si la cuenta ya existe, no la toca: se puede arrancar la app varias veces.
// Arranque: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
@Component
@Profile("dev")
public class DemoUsersInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsersInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final String staffPassword;
    private final String adminPassword;

    public DemoUsersInitializer(AuthService authService,
                                UserRepository userRepository,
                                UserRoleRepository userRoleRepository,
                                @Value("${PULSEPASS_DEMO_STAFF_PASSWORD:}") String staffPassword,
                                @Value("${PULSEPASS_DEMO_ADMIN_PASSWORD:}") String adminPassword) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.staffPassword = staffPassword;
        this.adminPassword = adminPassword;
    }

    // Una sola transacción: si algo falla, no queda ninguna cuenta a medias
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("laura", "laura@pulsepass.com", "Laura", "Staff", staffPassword, Role.STAFF);
        createIfMissing("admin", "admin@pulsepass.com", "Admin", "PulsePass", adminPassword, Role.ADMIN);
    }

    private void createIfMissing(String username, String email, String firstName, String lastName,
                                 String password, Role extraRole) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Cuenta demo '{}' ya existe: no se modifica", username);
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("Cuenta demo '{}' no creada: falta su contraseña en .env", username);
            return;
        }
        // Aquí no pasa por @Valid, así que se aplica a mano el mínimo del §9.3
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "La contraseña demo de '" + username + "' debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        // Mismo camino que el registro público: User + UserProfile + hash BCrypt + ROLE_USER
        UserResponse created = authService.register(new RegisterUserRequest(
                username, email, firstName, lastName, null, "Santa Marta", LocalDate.of(1995, 1, 1), password));

        // Y además el rol privilegiado
        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Cuenta demo recién creada no encontrada"));
        userRoleRepository.save(new UserRole(user, extraRole));

        log.info("Cuenta demo '{}' creada con roles USER y {}", username, extraRole);
    }
}
