package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.IReservaService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpleadoHabitacionesApiController.class)
@Import(SecurityConfig.class)
class EmpleadoHabitacionesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HabitacionRepository habitacionRepository;

    @MockitoBean
    private IReservaService reservaService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @Test
    void habitaciones_soloRecepcionYAdminPuedenConsultar() throws Exception {
        mockMvc.perform(get("/empleado/api/habitaciones")
                        .with(user("ana").roles("ROOMSERVICE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(habitacionRepository, reservaService);
    }

    @Test
    void habitacionesRecepcionVeEstadoDeBloqueoYMotivo() throws Exception {
        Habitacion habitacion = Habitacion.builder()
                .id(27)
                .numHabitacion("401")
                .piso((short) 4)
                .tipoHabitacion(TipoHabitacion.builder().nombreTematico("Suite").build())
                .estadoRack("BLOQUEADA")
                .motivoBloqueo("Mantenimiento")
                .build();
        when(habitacionRepository.findAllConTipo()).thenReturn(List.of(habitacion));

        mockMvc.perform(get("/empleado/api/habitaciones")
                        .with(user("lucia").roles("RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$[0].motivoBloqueo").value("Mantenimiento"));
    }

    @Test
    void bloquearHabitacionGuardaElMotivoConCsrf() throws Exception {
        Habitacion habitacion = Habitacion.builder()
                .id(27)
                .numHabitacion("401")
                .piso((short) 4)
                .tipoHabitacion(TipoHabitacion.builder().nombreTematico("Suite").build())
                .estadoRack("BLOQUEADA")
                .motivoBloqueo("Mantenimiento")
                .build();
        when(reservaService.bloquearHabitacion(27, "Mantenimiento")).thenReturn(habitacion);

        mockMvc.perform(post("/empleado/api/habitaciones/27/bloqueo")
                        .with(user("lucia").roles("RECEPCION"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Mantenimiento\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));

        verify(reservaService).bloquearHabitacion(27, "Mantenimiento");
    }

    @Test
    void bloquearHabitacionConflictoDevuelve409() throws Exception {
        when(reservaService.bloquearHabitacion(eq(27), any()))
                .thenThrow(new IllegalStateException("Tiene una reserva activa."));

        mockMvc.perform(post("/empleado/api/habitaciones/27/bloqueo")
                        .with(user("lucia").roles("RECEPCION"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Mantenimiento\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Tiene una reserva activa."));
    }
}
