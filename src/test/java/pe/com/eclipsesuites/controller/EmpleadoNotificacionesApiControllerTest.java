package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dao.ExtensionEstadiaRepository;
import pe.com.eclipsesuites.dto.response.PedidoItemResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.IRoomServiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpleadoNotificacionesApiController.class)
@Import(SecurityConfig.class)
class EmpleadoNotificacionesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaEstadiaRepository reservaEstadiaRepository;

    @MockitoBean
    private ExtensionEstadiaRepository extensionEstadiaRepository;

    @MockitoBean
    private IRoomServiceService roomServiceService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    private ReservaEstadia reservaCheckIn() {
        return ReservaEstadia.builder()
                .id(12L)
                .cliente(Cliente.builder().nombre("Ana").apellido("Torres").build())
                .tipoHabitacion(TipoHabitacion.builder().id(3).nombreTematico("Suite Panorámica").build())
                .habitacion(Habitacion.builder().id(9).numHabitacion("101").build())
                .cantidadPersonas(2)
                .montoTotal(new BigDecimal("170.00"))
                .estadoReserva(EstadoReserva.CHECKED_IN)
                .fechaCheckIn(LocalDateTime.now().minusMinutes(4))
                .fechaCreacion(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    private PedidoResponseDTO comanda() {
        return PedidoResponseDTO.builder()
                .codigo("A1488")
                .estado("RECIBIDO")
                .estadoEtiqueta("Recibido")
                .montoTotal(new BigDecimal("30.00"))
                .numHabitacion("101")
                .fHPedido(OffsetDateTime.now().minusMinutes(1))
                .items(List.of(PedidoItemResponseDTO.builder()
                        .nombre("Hamburguesa clásica")
                        .cantidad(2)
                        .subtotal(new BigDecimal("30.00"))
                        .build()))
                .build();
    }

    private void stubsBase() {
        when(reservaEstadiaRepository.buscarRecientesConCliente(any(Pageable.class)))
                .thenReturn(List.of(reservaCheckIn()));
        when(reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.PENDIENTE_PAGO))
                .thenReturn(1L);
        when(reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.CONFIRMADA))
                .thenReturn(2L);
        when(reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.CHECKED_IN))
                .thenReturn(3L);
        when(roomServiceService.listarPedidos()).thenReturn(List.of(comanda()));
        when(extensionEstadiaRepository.listarPendientesDeValidacion(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void notificaciones_sinSesionRetorna401() throws Exception {
        mockMvc.perform(get("/api/empleado/novedades"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaEstadiaRepository, extensionEstadiaRepository, roomServiceService);
    }

    @Test
    void notificaciones_clienteNoTienePermiso() throws Exception {
        mockMvc.perform(get("/api/empleado/novedades")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservaEstadiaRepository, extensionEstadiaRepository, roomServiceService);
    }

    @Test
    void notificaciones_recepcionVeReservasYComandas() throws Exception {
        stubsBase();

        mockMvc.perform(get("/api/empleado/novedades")
                        .with(user("lucia").roles("RECEPCION"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.reservasNuevas").value(1))
                .andExpect(jsonPath("$.resumen.pendientesCheckIn").value(2))
                .andExpect(jsonPath("$.resumen.reservasEnCurso").value(3))
                .andExpect(jsonPath("$.resumen.comandasActivas").value(1))
                .andExpect(jsonPath("$.notificaciones.length()").value(2))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("COMANDA"))
                .andExpect(jsonPath("$.notificaciones[0].titulo").value("Comanda A1488 · suite 101"))
                .andExpect(jsonPath("$.notificaciones[1].tipo").value("CHECK_IN"))
                .andExpect(jsonPath("$.notificaciones[1].estado").value("CHECKED_IN"))
                .andExpect(jsonPath("$.notificaciones[1].detalle")
                        .value("Suite Panorámica · suite 101 · 2 personas · S/ 170.00"));
    }

    @Test
    void notificaciones_roomserviceTambienTieneAcceso() throws Exception {
        stubsBase();

        mockMvc.perform(get("/api/empleado/novedades")
                        .with(user("miguel").roles("ROOMSERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("COMANDA"));
    }
}
