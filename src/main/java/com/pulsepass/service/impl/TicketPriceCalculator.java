package com.pulsepass.service.impl;

import com.pulsepass.model.TicketType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Estrategia de precio encapsulada (PRD Servicios §28, BR-TICKET-009).
// Precio fijo por tipo de ticket. Los valores de GENERAL y VIP salen de los PRD de
// frontend y persistencia; STUDENT y BACKSTAGE son decisión propia del autor del proyecto.
@Component
public class TicketPriceCalculator {

    private static final BigDecimal GENERAL_PRICE = new BigDecimal("120000.00");
    private static final BigDecimal VIP_PRICE = new BigDecimal("250000.00");
    private static final BigDecimal STUDENT_PRICE = new BigDecimal("60000.00");
    private static final BigDecimal BACKSTAGE_PRICE = new BigDecimal("400000.00");

    public BigDecimal calculate(TicketType type) {
        // switch sin default: si se agrega un TicketType nuevo, este código deja de compilar
        // hasta que se le asigne un precio, en lugar de fallar en ejecución.
        BigDecimal price = switch (type) {
            case GENERAL -> GENERAL_PRICE;
            case VIP -> VIP_PRICE;
            case STUDENT -> STUDENT_PRICE;
            case BACKSTAGE -> BACKSTAGE_PRICE;
        };
        // BR-TICKET-009: todos los precios son constantes positivas, nunca hay price < 0
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
