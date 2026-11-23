package pe.com.eclipsesuites.service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Generación de reportes de gestión en formato Excel (.xlsx).
 */
public interface IReporteExcelService {

    /** Zona horaria del hotel (Lima), usada para "hoy" y para delimitar el día del cierre. */
    ZoneId ZONA_HOTEL = ZoneId.of("America/Lima");

    /**
     * Cierre de caja de un día: resumen por método de pago y detalle de transacciones.
     *
     * @param fecha día a cerrar (obligatorio)
     * @return contenido del libro .xlsx
     */
    byte[] generarCierreCajaDiario(LocalDate fecha);

    /**
     * Estado actual de ocupación de las suites: resumen por estado del rack,
     * matriz por tipo de habitación y detalle por habitación.
     *
     * @return contenido del libro .xlsx
     */
    byte[] generarOcupacionSuites();
}
