package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DisponibilidadRateLimiterTest {

    @Test
    void permitir_limitaConsultasPorDireccionIp() {
        DisponibilidadRateLimiter rateLimiter = new DisponibilidadRateLimiter();

        for (int consulta = 0; consulta < 30; consulta++) {
            assertTrue(rateLimiter.permitir("192.0.2.1"));
        }
        assertFalse(rateLimiter.permitir("192.0.2.1"));
        assertTrue(rateLimiter.permitir("192.0.2.2"));
    }
}
