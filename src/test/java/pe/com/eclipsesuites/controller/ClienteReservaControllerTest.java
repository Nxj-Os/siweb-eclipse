package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.EstadoExtensionReserva;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.IReservaService;
import pe.com.eclipsesuites.service.QrCodeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = {ClienteReservaController.class, RecepcionReservaController.class,
        HomeController.class}, properties = "app.demo-mode=true")
@Import(SecurityConfig.class)
class ClienteReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IReservaService reservaService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @MockitoBean
    private QrCodeService qrCodeService;

    @Test
    void nuevaReservaSinSuiteRedirigeAlCatalogo() throws Exception {
        mockMvc.perform(get("/cliente/reservas/nueva")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/inicio"));
    }

    @Test
    void nuevaReservaMuestraSeleccionDeHabitacionDuracionYVistaPrevia() throws Exception {
        TipoHabitacion tipo = tipoHabitacion();
        configurarFormulario(tipo);

        mockMvc.perform(get("/cliente/reservas/nueva").param("tipoId", "3")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/reserva-paso1"))
                .andExpect(content().string(containsString("Elige habitación y horario")))
                .andExpect(content().string(containsString("Habitación 509")))
                .andExpect(content().string(containsString("Modo demo")))
                .andExpect(content().string(containsString("Vista previa")));
    }

    @Test
    void nuevaReservaRenderizaDatosDeSuiteTurnoYTarifa() throws Exception {
        TipoHabitacion tipo = tipoHabitacion();
        configurarFormulario(tipo);
        when(reservaService.listarTarifas()).thenReturn(List.of(
                TarifaHabitacion.builder()
                        .id(new TarifaHabitacionPK(3, 5))
                        .precioEstadiaBase(new BigDecimal("125.00"))
                        .vigenciaDesde(LocalDate.of(2026, 1, 1))
                        .vigenciaHasta(LocalDate.of(2027, 1, 1))
                        .build()));

        mockMvc.perform(get("/cliente/reservas/nueva").param("tipoId", "3")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Suite Galaxia")))
                .andExpect(content().string(containsString("Qué incluye")))
                .andExpect(content().string(containsString("data-precio=\"125.00\"")))
                .andExpect(content().string(containsString("4 horas")));
    }

    @Test
    void detalleMuestraEstadoYResumenDeLaReserva() throws Exception {
        ReservaEstadia reserva = reservaPendiente();
        when(reservaService.buscarReservaCliente(17L, "cliente@example.com"))
                .thenReturn(Optional.of(reserva));

        mockMvc.perform(get("/cliente/reservas/17")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/confirmacion-reserva"))
                .andExpect(content().string(containsString("Suite Galaxia")))
                .andExpect(content().string(containsString("S/ 125.00")));
    }

    @Test
    void pagoMuestraCuentaRegresivaYMetodosDemo() throws Exception {
        when(reservaService.buscarReservaCliente(17L, "cliente@example.com"))
                .thenReturn(Optional.of(reservaPendiente()));

        mockMvc.perform(get("/cliente/reservas/17/pago")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/reserva-pago"))
                .andExpect(content().string(containsString("Tiempo para pagar")))
                .andExpect(content().string(containsString("Simular pago y confirmar")))
                .andExpect(content().string(containsString("Yape")));
    }

    @Test
    void endpointDeDisponibilidadDevuelveIdsParaLaDuracionElegida() throws Exception {
        when(reservaService.listarModalidades()).thenReturn(List.of(
                ModalidadTurno.builder().id(5).duracionHoras(new BigDecimal("4.00")).build()));
        when(reservaService.listarHabitacionesDisponibles(
                3, LocalDateTime.of(2099, 6, 15, 18, 0),
                LocalDateTime.of(2099, 6, 15, 22, 0))).thenReturn(Set.of(27, 28));

        mockMvc.perform(get("/cliente/reservas/disponibilidad")
                        .param("tipoHabitacionId", "3")
                        .param("fechaReserva", "2099-06-15")
                        .param("horaInicio", "18:00")
                        .param("modalidadId", "5")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(content().string(containsString("27")))
                .andExpect(content().string(containsString("28")));
    }

    @Test
    void extensionDeEstadiaMuestraDuracionesDisponibles() throws Exception {
        ReservaEstadia reserva = reservaPendiente();
        reserva.setEstadoReserva(EstadoReserva.CHECKED_IN);
        when(reservaService.buscarReservaCliente(17L, "cliente@example.com"))
                .thenReturn(Optional.of(reserva));
        when(reservaService.listarModalidades()).thenReturn(List.of(
                ModalidadTurno.builder()
                        .id(5)
                        .nombreTurno("4 horas")
                        .duracionHoras(new BigDecimal("4.00"))
                        .build()));

        mockMvc.perform(get("/cliente/reservas/17/extensiones/nueva")
                        .with(user("cliente@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/extension-reserva"))
                .andExpect(content().string(containsString("¿Quieres más tiempo?")))
                .andExpect(content().string(containsString("4 horas")));
    }

    @Test
    void recepcionPuedeVerLaBandejaDeReservasYCheckIn() throws Exception {
        when(reservaService.listarReservasPendientes()).thenReturn(List.of());
        when(reservaService.listarReservasPendientesCheckIn()).thenReturn(List.of());

        mockMvc.perform(get("/recepcion/reservas")
                        .with(user("recepcion").roles("RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(view().name("recepcion/reservas-pendientes"))
                .andExpect(content().string(containsString("Solicitudes por validar")))
                .andExpect(content().string(containsString("Validar QR de check-in")));
    }

    @Test
    void inicioCargaCatalogoDinamicoYEnlaceDeReserva() throws Exception {
        TipoHabitacion tipo = tipoHabitacion();
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(tipo));
        when(reservaService.listarTarifas()).thenReturn(List.of(
                TarifaHabitacion.builder()
                        .id(new TarifaHabitacionPK(3, 5))
                        .precioEstadiaBase(new BigDecimal("120.00"))
                        .vigenciaDesde(LocalDate.now().minusDays(1))
                        .vigenciaHasta(LocalDate.now().plusDays(1))
                        .build()));

        mockMvc.perform(get("/inicio")
                        .with(user("recepcion").roles("RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Validar reservas")))
                .andExpect(content().string(containsString("Modo demo")))
                .andExpect(content().string(containsString("Suite Galaxia")))
                .andExpect(content().string(containsString("data-price=\"120.00\"")))
                .andExpect(content().string(containsString("/cliente/reservas/nueva?tipoId=3")));
    }

    private void configurarFormulario(TipoHabitacion tipo) {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(tipo));
        when(reservaService.listarModalidades()).thenReturn(List.of(
                ModalidadTurno.builder()
                        .id(5)
                        .nombreTurno("4 horas")
                        .duracionHoras(new BigDecimal("4.00"))
                        .horaInicioFija(LocalTime.of(18, 0))
                        .horaFinFija(LocalTime.of(22, 0))
                        .permiteAmanecida(false)
                        .build()));
        when(reservaService.listarHabitacionesPorTipo(3)).thenReturn(List.of(
                Habitacion.builder()
                        .id(27)
                        .tipoHabitacion(tipo)
                        .numHabitacion("509")
                        .estadoRack("LIBRE")
                        .build()));
        when(reservaService.listarHabitacionesDisponibles(
                eq(3), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Set.of(27));
        when(reservaService.listarTarifas()).thenReturn(List.of());
    }

    private TipoHabitacion tipoHabitacion() {
        return TipoHabitacion.builder()
                .id(3)
                .codTipo("GALAXIA")
                .nombreTematico("Suite Galaxia")
                .descripcionTematica("Vista estelar")
                .galeriaFotos("/img/suite-galaxia.webp")
                .capacidadMaxima((short) 2)
                .build();
    }

    private ReservaEstadia reservaPendiente() {
        return ReservaEstadia.builder()
                .id(17L)
                .cliente(Cliente.builder().correoElectronico("cliente@example.com").build())
                .tipoHabitacion(TipoHabitacion.builder().nombreTematico("Suite Galaxia").build())
                .habitacion(Habitacion.builder().numHabitacion("509").build())
                .modalidadTurno(ModalidadTurno.builder().nombreTurno("4 horas").build())
                .fechaInicio(LocalDateTime.of(2099, 6, 15, 18, 0))
                .fechaFin(LocalDateTime.of(2099, 6, 15, 22, 0))
                .cantidadPersonas(2)
                .montoTotal(new BigDecimal("125.00"))
                .metodoPago(MetodoPagoReserva.YAPE)
                .estadoReserva(EstadoReserva.PENDIENTE_PAGO)
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(5))
                .build();
    }
}
