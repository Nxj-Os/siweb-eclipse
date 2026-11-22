package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotRateLimiterTest {

    @Test
    void limitaPorClaveSinAfectarAOtras() {
        ChatbotRateLimiter rateLimiter = new ChatbotRateLimiter();

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.permitir("ip:192.0.2.1", 5));
        }
        assertFalse(rateLimiter.permitir("ip:192.0.2.1", 5));
        assertTrue(rateLimiter.permitir("ip:192.0.2.2", 5));
    }

    @Test
    void clavesInvalidasSiempreSeRechazan() {
        ChatbotRateLimiter rateLimiter = new ChatbotRateLimiter();

        assertFalse(rateLimiter.permitir(null, 10));
        assertFalse(rateLimiter.permitir("  ", 10));
        assertFalse(rateLimiter.permitir("ip:192.0.2.9", 0));
    }

    @Test
    void reiniciarLiberaLaClave() {
        ChatbotRateLimiter rateLimiter = new ChatbotRateLimiter();

        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.permitir("cliente:1", 3));
        }
        assertFalse(rateLimiter.permitir("cliente:1", 3));

        rateLimiter.reiniciar("cliente:1");

        assertTrue(rateLimiter.permitir("cliente:1", 3));
    }
}
