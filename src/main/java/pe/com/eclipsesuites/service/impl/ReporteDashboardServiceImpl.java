package pe.com.eclipsesuites.service.impl;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.TransaccionPagoRepository;
import pe.com.eclipsesuites.dto.DashboardReporte;
import pe.com.eclipsesuites.dto.DashboardReporte.HabitacionesPorEstado;
import pe.com.eclipsesuites.dto.DashboardReporte.IngresoFranja;
import pe.com.eclipsesuites.dto.DashboardReporte.IngresoHora;
import pe.com.eclipsesuites.dto.DashboardReporte.IngresoMetodo;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.TransaccionPago;
import pe.com.eclipsesuites.service.IReporteDashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteDashboardServiceImpl implements IReporteDashboardService {

    private static final String SIN_DATO = "SIN DATO";
    private static final int HORAS_POR_FRANJA = 6;
    private static final String[] NOMBRE_FRANJA = {"Madrugada", "Mañana", "Tarde", "Noche"};
    private static final String[] RANGO_FRANJA = {
            "00:00 - 05:59", "06:00 - 11:59", "12:00 - 17:59", "18:00 - 23:59"};
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final TransaccionPagoRepository transaccionPagoRepository;
    private final HabitacionRepository habitacionRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardReporte obtenerDashboard(LocalDate desde, LocalDate hasta) {
        Preconditions.checkArgument(desde != null && hasta != null, "El rango de fechas es obligatorio");
        Preconditions.checkArgument(!hasta.isBefore(desde),
                "La fecha final no puede ser anterior a la inicial");

        List<TransaccionPago> cobradas = transaccionPagoRepository
                .findByFechaTransaccionEntre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .filter(ReporteDashboardServiceImpl::esCobrada)
                .toList();

        // Ingresos acumulados por hora, por método y en total
        BigDecimal[] montoPorHora = new BigDecimal[24];
        Arrays.fill(montoPorHora, BigDecimal.ZERO);
        long[] cantidadPorHora = new long[24];
        TreeMap<String, Acumulado> porMetodo = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (TransaccionPago t : cobradas) {
            int hora = t.getFechaTransaccion().getHour();
            BigDecimal monto = montoDe(t);
            montoPorHora[hora] = montoPorHora[hora].add(monto);
            cantidadPorHora[hora]++;
            total = total.add(monto);
            Acumulado acumulado = porMetodo.computeIfAbsent(
                    normalizar(t.getComprobantePago().getMetodoPago()), k -> new Acumulado());
            acumulado.monto = acumulado.monto.add(monto);
            acumulado.cantidad++;
        }

        long totalTransacciones = cobradas.size();
        BigDecimal ticketPromedio = totalTransacciones == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(totalTransacciones), 2, RoundingMode.HALF_UP);

        // Barras por hora
        BigDecimal maximoHora = Arrays.stream(montoPorHora).reduce(BigDecimal.ZERO, BigDecimal::max);
        List<IngresoHora> porHora = new ArrayList<>(24);
        for (int hora = 0; hora < 24; hora++) {
            porHora.add(new IngresoHora(hora, montoPorHora[hora], cantidadPorHora[hora],
                    porcentaje(montoPorHora[hora], maximoHora)));
        }

        // Barras por franja
        int franjas = 24 / HORAS_POR_FRANJA;
        BigDecimal[] montoPorFranja = new BigDecimal[franjas];
        Arrays.fill(montoPorFranja, BigDecimal.ZERO);
        long[] cantidadPorFranja = new long[franjas];
        for (int hora = 0; hora < 24; hora++) {
            int franja = hora / HORAS_POR_FRANJA;
            montoPorFranja[franja] = montoPorFranja[franja].add(montoPorHora[hora]);
            cantidadPorFranja[franja] += cantidadPorHora[hora];
        }
        BigDecimal maximoFranja = Arrays.stream(montoPorFranja).reduce(BigDecimal.ZERO, BigDecimal::max);
        List<IngresoFranja> porFranja = new ArrayList<>(franjas);
        for (int i = 0; i < franjas; i++) {
            porFranja.add(new IngresoFranja(NOMBRE_FRANJA[i], RANGO_FRANJA[i], montoPorFranja[i],
                    cantidadPorFranja[i], porcentaje(montoPorFranja[i], maximoFranja)));
        }

        // Hora pico y hora valle: solo entre horas con movimiento
        Integer horaPico = null;
        Integer horaValle = null;
        for (int hora = 0; hora < 24; hora++) {
            if (cantidadPorHora[hora] == 0) {
                continue;
            }
            if (horaPico == null || montoPorHora[hora].compareTo(montoPorHora[horaPico]) > 0) {
                horaPico = hora;
            }
            if (horaValle == null || montoPorHora[hora].compareTo(montoPorHora[horaValle]) < 0) {
                horaValle = hora;
            }
        }

        List<IngresoMetodo> ingresosPorMetodo = porMetodo.entrySet().stream()
                .map(e -> new IngresoMetodo(e.getKey(), e.getValue().monto, e.getValue().cantidad))
                .toList();

        // Ocupación actual de habitaciones
        List<Habitacion> habitaciones = habitacionRepository.findAllConTipo();
        long totalHabitaciones = habitaciones.size();
        TreeMap<String, Long> porEstado = habitaciones.stream().collect(Collectors.groupingBy(
                h -> normalizar(h.getEstadoRack()), TreeMap::new, Collectors.counting()));
        long ocupadas = porEstado.getOrDefault(ReporteExcelServiceImpl.ESTADO_OCUPADA, 0L);
        double tasaOcupacion = totalHabitaciones == 0 ? 0d : (double) ocupadas / totalHabitaciones;
        List<HabitacionesPorEstado> habitacionesPorEstado = porEstado.entrySet().stream()
                .map(e -> new HabitacionesPorEstado(e.getKey(), e.getValue(),
                        totalHabitaciones == 0 ? 0 : (int) Math.round(100.0 * e.getValue() / totalHabitaciones)))
                .toList();

        log.info("Dashboard generado {} a {}: {} transacciones cobradas", desde, hasta, totalTransacciones);
        return new DashboardReporte(desde, hasta, total, totalTransacciones, ticketPromedio,
                porHora, porFranja, horaPico, horaValle, ingresosPorMetodo,
                totalHabitaciones, ocupadas, tasaOcupacion, habitacionesPorEstado);
    }

    // ------------------------------------------------------------------ Utilidades

    private static boolean esCobrada(TransaccionPago t) {
        return !ReporteExcelServiceImpl.ESTADOS_NO_COBRADOS.contains(normalizar(t.getEstado()));
    }

    private static BigDecimal montoDe(TransaccionPago t) {
        return t.getMonto() == null ? BigDecimal.ZERO : t.getMonto();
    }

    private static String normalizar(String valor) {
        String limpio = StringUtils.trimToEmpty(valor);
        return limpio.isEmpty() ? SIN_DATO : StringUtils.upperCase(limpio, Locale.ROOT);
    }

    /** Porcentaje de 0 a 100 respecto al máximo; un valor positivo nunca queda en 0 para que la barra se vea. */
    private static int porcentaje(BigDecimal valor, BigDecimal maximo) {
        if (maximo.signum() <= 0 || valor.signum() <= 0) {
            return 0;
        }
        int porcentaje = valor.multiply(CIEN).divide(maximo, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(1, porcentaje);
    }

    private static final class Acumulado {
        private BigDecimal monto = BigDecimal.ZERO;
        private long cantidad;
    }
}
