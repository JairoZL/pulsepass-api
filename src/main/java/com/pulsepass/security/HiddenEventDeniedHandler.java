package com.pulsepass.security;

import com.pulsepass.exception.ResourceNotFoundException;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.stereotype.Component;

// PRD Seguridad FR-SEC-EVT-02 y T-SEC-017: un evento no publicado no debe revelarse al público.
// Cuando el @PostAuthorize de EventService.findByCode niega el acceso, en vez de 401/403
// (que confirmarían que el evento existe) se responde exactamente el mismo 404 que da un
// código inexistente. GlobalExceptionHandler convierte ResourceNotFoundException en 404.
// Lo usa @HandleAuthorizationDenied(handlerClass = HiddenEventDeniedHandler.class).
// Debe ser un bean: Spring Security busca este handler en el contexto por su tipo
@Component
public class HiddenEventDeniedHandler implements MethodAuthorizationDeniedHandler {

    // Negación ANTES de ejecutar el método (no ocurre con @PostAuthorize, se deja por completitud)
    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        throw notFound(methodInvocation);
    }

    // Negación DESPUÉS de ejecutar el método: el caso de @PostAuthorize
    @Override
    public Object handleDeniedInvocationResult(MethodInvocationResult methodInvocationResult,
                                               AuthorizationResult authorizationResult) {
        throw notFound(methodInvocationResult.getMethodInvocation());
    }

    // Mismo mensaje que EventServiceImpl.findByCode cuando el código no existe
    private ResourceNotFoundException notFound(MethodInvocation methodInvocation) {
        return new ResourceNotFoundException("Event not found: " + methodInvocation.getArguments()[0]);
    }
}
