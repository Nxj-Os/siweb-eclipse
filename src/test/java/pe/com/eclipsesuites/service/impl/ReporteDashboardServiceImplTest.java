package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.TransaccionPagoRepository;
import pe.com.eclipsesuites.dto.DashboardReporte;
import pe.com.eclipsesuites.entity.ComprobantePago;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.entity.TransaccionPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteDashboardServiceImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 16);

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private HabitacionRepository habitacionRepository;

    @InjectMocks
    private ReporteDashboardServiceImpl servicio;

    @Test
    void ingresos_sumanSoloCobradasYSeReparteEnFranjasHorasYMetodos() {
        when(transaccionPagoRepository.findByFechaTransaccionEntre(any(), any())).thenReturn(List.of(
                transaccion("Efectivo", "APROBADO", "30.00", 2, 0),
                transaccion("Yape", "APROBADO", "50.00", 9, 0),
                transaccion("Efectivo", "APROBADO", "100.00", 20, 0),
                transaccion("Yape", "APROBADO", "40.00", 20, 30),
                transaccion("Yape", "ANULADO", "999.00", 19, 0)));
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of());

        DashboardReporte d = servicio.obtenerDashboard(DIA, DIA);

        assertIgual("220", d.totalCobrado());
        assertEquals(4, d.transaccionesCobradas());
        assertIgual("55.00", d.ticketPromedio());

        // Franjas: Madrugada, Mañana, Tarde, Noche
        assertEquals(4, d.ingresosPorFranja().size());
        assertIgual("30", d.ingresosPorFranja().get(0).monto());
        assertIgual("50", d.ingresosPorFranja().get(1).monto());
        assertIgual("0", d.ingresosPorFranja().get(2).monto());
        assertIgual("140", d.ingresosPorFranja().get(3).monto());
        assertEquals("Noche", d.ingresosPorFranja().get(3).nombre());
        assertEquals(100, d.ingresosPorFranja().get(3).porcentajeBarra());
        assertEquals(0, d.ingresosPorFranja().get(2).porcentajeBarra());

        // Horas
        assertEquals(24, d.ingresosPorHora().size());
        assertEquals(20, d.horaPico());
        assertEquals(2, d.horaValle());
        assertEquals("20:00 - 20:59", d.rangoHoraPico());
        assertEquals("02:00 - 02:59", d.rangoHoraValle());
        assertIgual("140", d.ingresosPorHora().get(20).monto());
        assertEquals(2, d.ingresosPorHora().get(20).transacciones());

        // Métodos de pago, ordenados por nombre
        assertEquals(2, d.ingresosPorMetodo().size());
        assertEquals("EFECTIVO", d.ingresosPorMetodo().get(0).metodo());
        assertIgual("130", d.ingresosPorMetodo().get(0).monto());
        assertEquals("YAPE", d.ingresosPorMetodo().get(1).metodo());
        assertIgual("90", d.ingresosPorMetodo().get(1).monto());
        assertEquals(2, d.ingresosPorMetodo().get(1).transacciones());
    }

    @Test
    void sinTransacciones_devuelveCerosYSinHorasPicoNiValle() {
        when(transaccionPagoRepository.findByFechaTransaccionEntre(any(), any())).thenReturn(List.of());
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of());

        DashboardReporte d = servicio.obtenerDashboard(DIA, DIA);

        assertIgual("0", d.totalCobrado());
        assertEquals(0, d.transaccionesCobradas());
        assertIgual("0", d.ticketPromedio());
        assertNull(d.horaPico());
        assertNull(d.horaValle());
        assertEquals("Sin datos", d.rangoHoraPico());
        assertEquals(24, d.ingresosPorHora().size());
        assertEquals(4, d.ingresosPorFranja().size());
        assertEquals(0, d.tasaOcupacion(), 0.0001);
        assertEquals(0, d.totalHabitaciones());
    }

    @Test
    void consultaDesdeElInicioDelPrimerDiaHastaElFinDelUltimo() {
        LocalDate hasta = DIA.plusDays(2);
        when(transaccionPagoRepository.findByFechaTransaccionEntre(
                DIA.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of());

        DashboardReporte d = servicio.obtenerDashboard(DIA, hasta);

        assertEquals(DIA, d.desde());
        assertEquals(hasta, d.hasta());
    }

    @Test
    void ocupacion_calculaTasaYPorcentajePorEstado() {
        TipoHabitacion tipo = TipoHabitacion.builder().nombreTematico("Suite Panorámica").build();
        when(transaccionPagoRepository.findByFechaTransaccionEntre(any(), any())).thenReturn(List.of());
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of(
                habitacion("101", tipo, "OCUPADA"),
                habitacion("102", tipo, "DISPONIBLE"),
                habitacion("201", tipo, "ocupada")));

        DashboardReporte d = servicio.obtenerDashboard(DIA, DIA);

        assertEquals(3, d.totalHabitaciones());
        assertEquals(2, d.habitacionesOcupadas());
        assertEquals(2.0 / 3.0, d.tasaOcupacion(), 0.0001);
        assertEquals(2, d.habitacionesPorEstado().size());
        assertEquals("DISPONIBLE", d.habitacionesPorEstado().get(0).estado());
        assertEquals(33, d.habitacionesPorEstado().get(0).porcentaje());
        assertEquals("OCUPADA", d.habitacionesPorEstado().get(1).estado());
        assertEquals(67, d.habitacionesPorEstado().get(1).porcentaje());
    }

    @Test
    void rechazaRangoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerDashboard(null, DIA));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerDashboard(DIA, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerDashboard(DIA, DIA.minusDays(1)));
        verifyNoInteractions(transaccionPagoRepository, habitacionRepository);
    }

    // ---------------------------------------------------------------- utilidades

    private static void assertIgual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "Se esperaba " + esperado + " pero fue " + real);
    }

    private static TransaccionPago transaccion(String metodo, String estado, String monto,
                                               int hora, int minuto) {
        ComprobantePago comprobante = ComprobantePago.builder()
                .numeroComprobante("B001-" + hora + minuto)
                .metodoPago(metodo)
                .build();
        return TransaccionPago.builder()
                .comprobantePago(comprobante)
                .tipoTransaccion("PAGO")
                .monto(new BigDecimal(monto))
                .estado(estado)
                .fechaTransaccion(LocalDateTime.of(DIA, LocalTime.of(hora, minuto)))
                .build();
    }

    private static Habitacion habitacion(String numero, TipoHabitacion tipo, String estado) {
        return Habitacion.builder()
                .numHabitacion(numero)
                .piso((short) 1)
                .tipoHabitacion(tipo)
                .estadoRack(estado)
                .build();
    }
}
