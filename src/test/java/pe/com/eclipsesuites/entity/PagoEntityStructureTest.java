package pe.com.eclipsesuites.entity;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoEntityStructureTest {

    @Test
    void comprobantePagoShouldBeJpaEntity() {
        assertTrue(ComprobantePago.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void transaccionPagoShouldBeJpaEntity() {
        assertTrue(TransaccionPago.class.isAnnotationPresent(Entity.class));
    }
}
