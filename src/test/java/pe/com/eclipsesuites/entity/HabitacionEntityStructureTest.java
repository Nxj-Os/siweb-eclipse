package pe.com.eclipsesuites.entity;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HabitacionEntityStructureTest {

    @Test
    void tipoHabitacionShouldBeJpaEntity() {
        assertTrue(TipoHabitacion.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void habitacionShouldBeJpaEntity() {
        assertTrue(Habitacion.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void modalidadTurnoShouldBeJpaEntity() {
        assertTrue(ModalidadTurno.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void tarifaHabitacionShouldBeJpaEntity() {
        assertTrue(TarifaHabitacion.class.isAnnotationPresent(Entity.class));
    }
}
