package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dto.response.DisponibilidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.HabitacionCatalogoDTO;
import pe.com.eclipsesuites.dto.response.ModalidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteDetalleDTO;
import pe.com.eclipsesuites.dto.response.TarifaCatalogoDTO;
import pe.com.eclipsesuites.service.ICatalogoService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.impl.DisponibilidadRateLimiter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CatalogoController.class)
@Import(SecurityConfig.class)
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICatalogoService catalogoService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @MockitoBean
    private DisponibilidadRateLimiter disponibilidadRateLimiter;

    @Test
    void catalogoEsPublicoYMuestraSuitesConDisponibilidadReal() throws Exception {
        when(catalogoService.listarSuites(null, null, null)).thenReturn(List.of(suite(true, 2)));
        when(catalogoService.listarModalidades()).thenReturn(List.of(modalidad()));

        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo/index"))
                .andExpect(content().string(containsString("Catálogo de suites")))
                .andExpect(content().string(containsString("Suite Panorámica")))
                .andExpect(content().string(containsString("2 de 8 disponibles")))
                .andExpect(content().string(containsString("S/ 120.00")))
                .andExpect(content().string(containsString("/catalogo/PANORAMICA")))
                .andExpect(content().string(containsString("/cliente/reservas/nueva?tipoId=3")));
    }

    @Test
    void catalogoMuestraEstadoVacioCuandoNoHaySuites() throws Exception {
        when(catalogoService.listarSuites(null, null, null)).thenReturn(List.of());
        when(catalogoService.listarModalidades()).thenReturn(List.of());

        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No encontramos suites con esos filtros")));
    }

    @Test
    void catalogoFiltraPorTextoYSoloDisponibles() throws Exception {
        when(catalogoService.listarSuites(null, null, null)).thenReturn(List.of(
                suite(true, 2),
                SuiteCatalogoDTO.builder()
                        .id(4)
                        .codTipo("ROJA")
                        .nombre("Suite Roja")
                        .descripcion("Ambientación roja.")
                        .imagen("/img/suite-placeholder.svg")
                        .capacidadMaxima((short) 2)
                        .habitacionesTotales(1)
                        .habitacionesDisponibles(0)
                        .disponible(false)
                        .precioDesde(new BigDecimal("100.00"))
                        .modalidadDesde("4 horas")
                        .amenidades(List.of("Wi-Fi"))
                        .tarifas(List.of())
                        .build()));
        when(catalogoService.listarModalidades()).thenReturn(List.of(modalidad()));

        mockMvc.perform(get("/catalogo").param("soloDisponibles", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Suite Panorámica")))
                .andExpect(content().string(not(containsString("Suite Roja"))));
    }

    @Test
    void catalogoFiltraPorTextoEnServidor() throws Exception {
        when(catalogoService.listarSuites(null, null, null)).thenReturn(List.of(
                suite(true, 2),
                SuiteCatalogoDTO.builder()
                        .id(4)
                        .codTipo("ROJA")
                        .nombre("Suite Roja")
                        .descripcion("Ambientación roja.")
                        .imagen("/img/suite-placeholder.svg")
                        .capacidadMaxima((short) 2)
                        .habitacionesTotales(1)
                        .habitacionesDisponibles(1)
                        .disponible(true)
                        .precioDesde(new BigDecimal("100.00"))
                        .modalidadDesde("4 horas")
                        .amenidades(List.of("Wi-Fi"))
                        .tarifas(List.of())
                        .build()));
        when(catalogoService.listarModalidades()).thenReturn(List.of(modalidad()));

        mockMvc.perform(get("/catalogo").param("q", "roja"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Suite Roja")))
                .andExpect(content().string(not(containsString("Suite Panorámica"))));
    }

    @Test
    void detalleDeSuiteEsPublico() throws Exception {
        when(catalogoService.obtenerDetalle("PANORAMICA", null, null, null))
                .thenReturn(Optional.of(detalle()));

        mockMvc.perform(get("/catalogo/PANORAMICA"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo/detalle"))
                .andExpect(content().string(containsString("Qué incluye")))
                .andExpect(content().string(containsString("Wi-Fi")))
                .andExpect(content().string(containsString("N.º 509")))
                .andExpect(content().string(containsString("Reservar esta suite")));
    }

    @Test
    void detalleDeSuiteInexistenteDevuelve404() throws Exception {
        when(catalogoService.obtenerDetalle("NO_EXISTE", null, null, null))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/catalogo/NO_EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiCatalogoEsPublicaYDevuelveJson() throws Exception {
        when(catalogoService.listarSuites(null, null, null)).thenReturn(List.of(suite(true, 2)));

        mockMvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codTipo").value("PANORAMICA"))
                .andExpect(jsonPath("$[0].nombre").value("Suite Panorámica"))
                .andExpect(jsonPath("$[0].habitacionesDisponibles").value(2))
                .andExpect(jsonPath("$[0].precioDesde").value(120.00))
                .andExpect(jsonPath("$[0].amenidades[0]").value("Wi-Fi"));
    }

    @Test
    void apiDetalleDevuelveTarifasYHabitaciones() throws Exception {
        when(catalogoService.obtenerDetalle("PANORAMICA", null, null, null))
                .thenReturn(Optional.of(detalle()));

        mockMvc.perform(get("/api/catalogo/PANORAMICA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codTipo").value("PANORAMICA"))
                .andExpect(jsonPath("$.tarifas[0].modalidad").value("4 horas"))
                .andExpect(jsonPath("$.habitaciones[0].numero").value("509"))
                .andExpect(jsonPath("$.habitaciones[0].disponible").value(true));
    }

    @Test
    void apiDisponibilidadEsPublicaYDevuelveResultado() throws Exception {
        when(disponibilidadRateLimiter.permitir(anyString())).thenReturn(true);
        when(catalogoService.consultarDisponibilidad(eq(3), any(LocalDate.class),
                any(LocalTime.class), eq(1))).thenReturn(DisponibilidadCatalogoDTO.builder()
                .tipoHabitacionId(3)
                .codTipo("PANORAMICA")
                .nombre("Suite Panorámica")
                .fecha(LocalDate.of(2099, 6, 15))
                .horaInicio(LocalTime.of(18, 0))
                .horaFin(LocalTime.of(22, 0))
                .modalidadId(1)
                .modalidad("4 horas")
                .habitacionesTotales(8)
                .habitacionesDisponibles(2)
                .disponible(true)
                .idsHabitacion(List.of(27, 29))
                .numerosHabitacion(List.of("509", "512"))
                .build());

        mockMvc.perform(get("/api/catalogo/disponibilidad")
                        .param("tipoHabitacionId", "3")
                        .param("fecha", "2099-06-15")
                        .param("hora", "18:00")
                        .param("modalidadId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.habitacionesDisponibles").value(2))
                .andExpect(jsonPath("$.numerosHabitacion[0]").value("509"));
    }

    @Test
    void apiDisponibilidadAplicaLimiteDeConsultas() throws Exception {
        when(disponibilidadRateLimiter.permitir(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/catalogo/disponibilidad")
                        .param("tipoHabitacionId", "3")
                        .param("fecha", "2099-06-15")
                        .param("hora", "18:00"))
                .andExpect(status().isTooManyRequests());
    }

    private SuiteCatalogoDTO suite(boolean disponible, int disponibles) {
        return SuiteCatalogoDTO.builder()
                .id(3)
                .codTipo("PANORAMICA")
                .nombre("Suite Panorámica")
                .descripcion("Vista panorámica al exterior.")
                .imagen("/img/suite-galaxia.webp")
                .capacidadMaxima((short) 2)
                .habitacionesTotales(8)
                .habitacionesDisponibles(disponibles)
                .disponible(disponible)
                .precioDesde(new BigDecimal("120.00"))
                .modalidadDesde("4 horas")
                .amenidades(List.of("Wi-Fi", "Smart TV"))
                .tarifas(List.of(tarifa()))
                .build();
    }

    private SuiteDetalleDTO detalle() {
        return SuiteDetalleDTO.builder()
                .id(3)
                .codTipo("PANORAMICA")
                .nombre("Suite Panorámica")
                .descripcion("Vista panorámica al exterior.")
                .imagen("/img/suite-galaxia.webp")
                .capacidadMaxima((short) 2)
                .habitacionesTotales(8)
                .habitacionesDisponibles(2)
                .disponible(true)
                .precioDesde(new BigDecimal("120.00"))
                .modalidadDesde("4 horas")
                .amenidades(List.of("Wi-Fi", "Smart TV", "Frigobar"))
                .tarifas(List.of(tarifa()))
                .habitaciones(List.of(
                        HabitacionCatalogoDTO.builder().id(27).numero("509").piso((short) 5)
                                .estadoRack("LIBRE").disponible(true).build(),
                        HabitacionCatalogoDTO.builder().id(28).numero("510").piso((short) 5)
                                .estadoRack("OCUPADA").disponible(false).build()))
                .build();
    }

    private TarifaCatalogoDTO tarifa() {
        return TarifaCatalogoDTO.builder()
                .modalidadId(1)
                .modalidad("4 horas")
                .duracionHoras(new BigDecimal("4.00"))
                .precio(new BigDecimal("120.00"))
                .costoHoraAdicional(BigDecimal.ZERO)
                .vigente(true)
                .build();
    }

    private ModalidadCatalogoDTO modalidad() {
        return ModalidadCatalogoDTO.builder()
                .id(1)
                .nombre("4 horas")
                .duracionHoras(new BigDecimal("4.00"))
                .build();
    }
}
