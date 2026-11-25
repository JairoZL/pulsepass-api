package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.security.HiddenEventDeniedHandler;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.util.List;

// Contrato: PRD Servicios §11
// Seguridad (PRD Seguridad FR-SEC-EVT-02/06): el público solo ve eventos PUBLISHED, igual que
// GET /api/events/published. STAFF y ADMIN siguen viendo todos los estados.
// Los roles de create/publish/addArtist (ADMIN) los exige SecurityConfig por URL.
public interface EventService {

    EventResponse create(CreateEventRequest request);

    // FR-SEC-EVT-02. @PostAuthorize se evalúa DESPUÉS de ejecutar: necesita el estado del evento
    // (returnObject). Si niega, HiddenEventDeniedHandler responde el mismo 404 de un código
    // inexistente, así el público no puede saber que hay un borrador con ese código (T-SEC-017)
    @PostAuthorize("returnObject.status().name() == 'PUBLISHED' or hasAnyRole('STAFF', 'ADMIN')")
    @HandleAuthorizationDenied(handlerClass = HiddenEventDeniedHandler.class)
    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    EventResponse publish(String eventCode);

    EventResponse addArtist(String eventCode, Long artistId);

    // FR-SEC-EVT-06. @PostFilter recorre la lista devuelta y quita cada elemento (filterObject)
    // que no cumpla la condición. Requiere que la implementación devuelva una lista modificable
    @PostFilter("filterObject.status().name() == 'PUBLISHED' or hasAnyRole('STAFF', 'ADMIN')")
    List<EventSummaryResponse> findByArtist(String stageName);
}
