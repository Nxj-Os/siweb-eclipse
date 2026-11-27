package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import pe.com.eclipsesuites.dto.DashboardReporte;
import pe.com.eclipsesuites.service.IReporteDashboardService;
import pe.com.eclipsesuites.service.IReporteExcelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportePanelControllerTest {

    @Mock
    private IReporteDashboardService servicio;

    private ReportePanelController controlador;
    private ConcurrentModel modelo;
    private LocalDate hoy;

    @BeforeEach
    void preparar() {
        controlador = new ReportePanelController(servicio);
        modelo = new ConcurrentModel();
        hoy = LocalDate.now(IReporteExcelService.ZONA_HOTEL);
        when(servicio.obtenerDashboard(any(), any())).thenAnswer(invocacion ->
                dashboardVacio(invocacion.getArgument(0), invocacion.getArgument(1)));
    }

    @Test
    void sinParametros_usaLosUltimosSieteDiasHastaHoy() {
        String vista = controlador.panel(null, null, modelo);

        assertEquals("reportes/dashboard", vista);
        assertEquals(hoy, modelo.getAttribute("hoy"));
        assertNotNull(modelo.getAttribute("dash"));
        assertRango(hoy.minusDays(6), hoy);
    }

    @Test
    void respetaUnRangoValido() {
        LocalDate desde = hoy.minusDays(20);
        LocalDate hasta = hoy.minusDays(10);

        controlador.panel(desde, hasta, modelo);

        assertRango(desde, hasta);
    }

    @Test
    void intercambiaUnRangoInvertido() {
        LocalDate desde = hoy.minusDays(2);
        LocalDate hasta = hoy.minusDays(9);

        controlador.panel(desde, hasta, modelo);

        assertRango(hasta, desde);
    }

    @Test
    void recortaLaFechaFinalFuturaAHoy() {
        controlador.panel(hoy.minusDays(3), hoy.plusDays(30), modelo);

        assertRango(hoy.minusDays(3), hoy);
    }

    @Test
    void siSoloHayFechaFinalMuestraSieteDiasHaciaAtras() {
        LocalDate hasta = hoy.minusDays(5);

        controlador.panel(null, hasta, modelo);

        assertRango(hasta.minusDays(6), hasta);
    }

    @Test
    void limitaElRangoAlMaximoPermitido() {
        controlador.panel(hoy.minusDays(2000), hoy, modelo);

        assertRango(hoy.minusDays(ReportePanelController.DIAS_MAXIMOS - 1L), hoy);
    }

    @Test
    void siLaFechaInicialEsFuturaSeAjustaAHoy() {
        controlador.panel(hoy.plusDays(10), null, modelo);

        assertRango(hoy, hoy);
    }

    // ---------------------------------------------------------------- utilidades

    private void assertRango(LocalDate desdeEsperado, LocalDate hastaEsperado) {
        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> hasta = ArgumentCaptor.forClass(LocalDate.class);
        verify(servicio).obtenerDashboard(desde.capture(), hasta.capture());
        assertEquals(desdeEsperado, desde.getValue());
        assertEquals(hastaEsperado, hasta.getValue());
    }

    private static DashboardReporte dashboardVacio(LocalDate desde, LocalDate hasta) {
        return new DashboardReporte(desde, hasta, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                List.of(), List.of(), null, null, List.of(), 0, 0, 0d, List.of());
    }
}
