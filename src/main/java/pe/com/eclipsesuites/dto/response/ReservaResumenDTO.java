package pe.com.eclipsesuites.dto.response;

import java.math.BigDecimal;

/**
 * Resumen de una reserva tal como se muestra en el perfil del cliente.
 */
public record ReservaResumenDTO(
        Long id,
        String codigo,
        String suite,
        String tipoHabitacion,
        String habitacion,
        String estado,
        String estadoEtiqueta,
        String fechaInicio,
        String fechaFin,
        String modalidad,
        Integer cantidadPersonas,
        String metodoPago,
        BigDecimal montoTotal,
        String fechaCreacion,
        String imagen,
        boolean activa) {
}
