package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ConcurrentModel;
import pe.com.eclipsesuites.service.IReporteExcelService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la lógica del controlador sin levantar Spring Security: la regla de rol ADMIN
 * para /reportes/** vive en SecurityConfig y se verifica por separado.
 */
@ExtendWith(MockitoExtension.class)
class ReporteExcelControllerTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private IReporteExcelService servicio;

    private ReporteExcelController controlador;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        controlador = new ReporteExcelController(servicio);
        mvc = MockMvcBuilders.standaloneSetup(controlador).build();
    }

    @Test
    void panel_devuelveLaVistaConLaFechaDeHoy() {
        ConcurrentModel modelo = new ConcurrentModel();

        String vista = controlador.panel(modelo);

        assertEquals("reportes/panel-reportes", vista);
        assertEquals(LocalDate.now(IReporteExcelService.ZONA_HOTEL), modelo.getAttribute("hoy"));
    }

    @Test
    void cierreCaja_descargaExcelConNombreDeLaFecha() throws Exception {
        LocalDate ayer = LocalDate.now(IReporteExcelService.ZONA_HOTEL).minusDays(1);
        byte[] contenido = {1, 2, 3};
        when(servicio.generarCierreCajaDiario(ayer)).thenReturn(contenido);

        mvc.perform(get("/reportes/cierre-caja").param("fecha", ayer.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(contieneNombre("cierre-caja-" + ayer + ".xlsx"))
                .andExpect(content().bytes(contenido));
    }

    @Test
    void cierreCaja_sinFechaUsaHoy() throws Exception {
        LocalDate hoy = LocalDate.now(IReporteExcelService.ZONA_HOTEL);
        when(servicio.generarCierreCajaDiario(hoy)).thenReturn(new byte[]{9});

        mvc.perform(get("/reportes/cierre-caja"))
                .andExpect(status().isOk())
                .andExpect(contieneNombre("cierre-caja-" + hoy + ".xlsx"));
    }

    @Test
    void cierreCaja_rechazaFechaFutura() throws Exception {
        LocalDate manana = LocalDate.now(IReporteExcelService.ZONA_HOTEL).plusDays(1);

        mvc.perform(get("/reportes/cierre-caja").param("fecha", manana.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void cierreCaja_rechazaFechaConFormatoInvalido() throws Exception {
        mvc.perform(get("/reportes/cierre-caja").param("fecha", "17-11-2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void ocupacion_descargaExcel() throws Exception {
        byte[] contenido = {4, 5};
        when(servicio.generarOcupacionSuites()).thenReturn(contenido);

        String disposicion = mvc.perform(get("/reportes/ocupacion"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(content().bytes(contenido))
                .andReturn().getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(disposicion != null && disposicion.contains("ocupacion-suites-"));
    }

    private static ResultMatcher contieneNombre(String nombreArchivo) {
        return resultado -> {
            String disposicion = resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
            assertTrue(disposicion != null && disposicion.contains(nombreArchivo),
                    "Content-Disposition inesperado: " + disposicion);
        };
    }
}
