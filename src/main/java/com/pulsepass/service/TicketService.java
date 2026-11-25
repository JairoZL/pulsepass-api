package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

// Contrato: PRD Servicios §21
// Seguridad (PRD Seguridad §11 y §11.1): propiedad verificada a nivel de método, no solo por URL.
// @PreAuthorize se evalúa ANTES de ejecutar el método: si niega, el Service nunca corre.
// "@authz" es AuthorizationService; "#request" / "#ticketCode" / "#email" son los parámetros.
// Los roles por URL (quién puede llamar cada ruta) siguen en SecurityConfig.
public interface TicketService {

    // FR-SEC-TKT-01: solo USER, y el userEmail del body debe ser el suyo
    @PreAuthorize("hasRole('USER') and @authz.isSelfEmail(#request.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    // FR-SEC-TKT-02: el dueño, o STAFF/ADMIN (STAFF global en el MVP, §11.1)
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or @authz.isTicketOwner(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    // FR-SEC-TKT-03: el dueño de ese correo, o ADMIN
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelfEmail(#email)")
    List<TicketResponse> findByUserEmail(String email);

    // FR-SEC-TKT-04: el rol STAFF/ADMIN ya lo exige SecurityConfig por URL
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    // FR-SEC-TKT-05: el dueño, o ADMIN. Se verifica ANTES de cambiar el estado;
    // las reglas de cancelación del Service (solo PAID, antes del evento) siguen igual
    @PreAuthorize("hasRole('ADMIN') or @authz.isTicketOwner(#ticketCode)")
    TicketResponse cancel(String ticketCode);

    // FR-SEC-TKT-06: el rol STAFF/ADMIN ya lo exige SecurityConfig por URL
    TicketResponse markAsUsed(String ticketCode);
}
