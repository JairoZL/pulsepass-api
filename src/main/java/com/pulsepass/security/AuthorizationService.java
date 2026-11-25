package com.pulsepass.security;

import com.pulsepass.model.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// PRD Seguridad §7.1 y §11.1 (anti-IDOR): responde "¿este recurso es del usuario autenticado?".
// Se usa desde las anotaciones de las interfaces de Service, con el nombre de bean "authz":
//   @PreAuthorize("hasRole('ADMIN') or @authz.isTicketOwner(#ticketCode)")
// Siempre compara IDs PERSISTENTES leídos de la base contra el userId del token (SEC-006).
// Nunca confía en el email o username que manda el cliente: solo los usa para BUSCAR en la base.
// No decide reglas de negocio (capacidad, edad, estados): eso sigue en los Services.
@Service("authz")
public class AuthorizationService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // ¿El correo pedido es el del usuario autenticado? (FR-SEC-TKT-01, TKT-03, USR-02)
    // Un correo que no existe devuelve false: el cliente recibe 403, igual que con uno ajeno
    public boolean isSelfEmail(String email) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || email == null) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getId)
                .map(currentUserId::equals)
                .orElse(false);
    }

    // ¿El username pedido es el del usuario autenticado? (FR-SEC-USR-03)
    public boolean isSelfUsername(String username) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || username == null) {
            return false;
        }
        return userRepository.findByUsername(username)
                .map(User::getId)
                .map(currentUserId::equals)
                .orElse(false);
    }

    // ¿El ticket pertenece al usuario autenticado? (FR-SEC-TKT-02, TKT-05)
    // El dueño se determina en la base (tickets.user_id), nunca por el ticketCode solo (§11.1).
    // Ticket inexistente -> false -> 403: un USER no puede averiguar qué códigos existen
    public boolean isTicketOwner(String ticketCode) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || ticketCode == null) {
            return false;
        }
        return ticketRepository.existsByTicketCodeAndUserId(ticketCode, currentUserId);
    }

    // El userId lo puso JwtAuthenticationFilter en el SecurityContext. Si no hay un
    // SecurityPrincipal (anónimo u otro tipo de autenticación), no hay dueño posible
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
