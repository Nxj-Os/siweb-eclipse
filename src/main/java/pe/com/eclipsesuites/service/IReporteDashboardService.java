package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.DashboardReporte;

import java.time.LocalDate;

/**
 * Consolida los indicadores del dashboard gerencial de reportes.
 */
public interface IReporteDashboardService {

    /**
     * Calcula ingresos (total, por franja, por hora y por método de pago) del rango indicado,
     * ambos extremos incluidos, y el estado actual de ocupación de las habitaciones.
     *
     * @param desde primer día del rango (obligatorio)
     * @param hasta último día del rango (obligatorio, no anterior a {@code desde})
     */
    DashboardReporte obtenerDashboard(LocalDate desde, LocalDate hasta);
}
