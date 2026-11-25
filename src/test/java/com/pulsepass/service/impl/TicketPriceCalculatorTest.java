package com.pulsepass.service.impl;

import com.pulsepass.model.TicketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Estrategia de precio probada (PRD Servicios §28, BR-TICKET-009). Sin Spring ni mocks.
class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    @ParameterizedTest
    @CsvSource({
            "GENERAL,   120000.00",
            "VIP,       250000.00",
            "STUDENT,    60000.00",
            "BACKSTAGE, 400000.00"
    })
    @DisplayName("Cada tipo devuelve su precio fijo con escala 2")
    void calculate_returnsFixedPricePerType(TicketType type, String expected) {
        // ACT
        BigDecimal price = calculator.calculate(type);

        // ASSERT
        assertThat(price).isEqualByComparingTo(expected);
        assertThat(price.scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(TicketType.class)
    @DisplayName("BR-TICKET-009: ningún tipo produce un precio negativo")
    void calculate_neverReturnsNegativePrice(TicketType type) {
        // ACT
        BigDecimal price = calculator.calculate(type);

        // ASSERT
        assertThat(price).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("El orden de precios es STUDENT < GENERAL < VIP < BACKSTAGE")
    void calculate_pricesAreOrdered() {
        assertThat(calculator.calculate(TicketType.STUDENT))
                .isLessThan(calculator.calculate(TicketType.GENERAL));
        assertThat(calculator.calculate(TicketType.GENERAL))
                .isLessThan(calculator.calculate(TicketType.VIP));
        assertThat(calculator.calculate(TicketType.VIP))
                .isLessThan(calculator.calculate(TicketType.BACKSTAGE));
    }
}
