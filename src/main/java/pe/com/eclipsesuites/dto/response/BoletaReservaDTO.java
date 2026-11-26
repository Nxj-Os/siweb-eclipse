package pe.com.eclipsesuites.dto.response;

import java.math.BigDecimal;

/**
 * Boleta de una reserva con el desglose de IGV que muestra el perfil.
 */
public record BoletaReservaDTO(
        Long idReserva,
        String numeroBoleta,
        String hotel,
        String cliente,
        String tipoDocumento,
        String numDocumento,
        String correoElectronico,
        String suite,
        String habitacion,
        String modalidad,
        String fechaInicio,
        String fechaFin,
        String estado,
        String metodoPago,
        Integer cantidadPersonas,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        String fechaEmision) {
}
