package pe.com.eclipsesuites.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Datos que muestra el dashboard gerencial de reportes para un rango de fechas.
 * Los ingresos consideran solo transacciones cobradas.
 */
public record DashboardReporte(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalCobrado,
        long transaccionesCobradas,
        BigDecimal ticketPromedio,
        List<IngresoHora> ingresosPorHora,
        List<IngresoFranja> ingresosPorFranja,
        Integer horaPico,
        Integer horaValle,
        List<IngresoMetodo> ingresosPorMetodo,
        long totalHabitaciones,
        long habitacionesOcupadas,
        double tasaOcupacion,
        List<HabitacionesPorEstado> habitacionesPorEstado) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Rango en texto, por ejemplo "17/11/2026 - 23/11/2026". */
    public String rangoTexto() {
        return desde.format(FORMATO_FECHA) + " - " + hasta.format(FORMATO_FECHA);
    }

    /** Rango de la hora con más ingresos, por ejemplo "20:00 - 20:59". */
    public String rangoHoraPico() {
        return rango(horaPico);
    }

    /** Rango de la hora con menos ingresos entre las horas con movimiento. */
    public String rangoHoraValle() {
        return rango(horaValle);
    }

    private static String rango(Integer hora) {
        return hora == null ? "Sin datos" : String.format(Locale.ROOT, "%02d:00 - %02d:59", hora, hora);
    }

    /** Ingresos de una hora del día (0 a 23). {@code porcentajeBarra} va de 0 a 100. */
    public record IngresoHora(int hora, BigDecimal monto, long transacciones, int porcentajeBarra) {

        public String etiqueta() {
            return String.format(Locale.ROOT, "%02d", hora);
        }

        public String descripcion() {
            return String.format(Locale.ROOT, "%02d:00 · S/ %.2f · %d transacciones",
                    hora, monto, transacciones);
        }
    }

    /** Ingresos de una franja (madrugada, mañana, tarde, noche). */
    public record IngresoFranja(String nombre, String rango, BigDecimal monto,
                                long transacciones, int porcentajeBarra) {
    }

    public record IngresoMetodo(String metodo, BigDecimal monto, long transacciones) {
    }

    public record HabitacionesPorEstado(String estado, long cantidad, int porcentaje) {
    }
}
