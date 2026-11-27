package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dto.request.PerfilClienteUpdateDTO;
import pe.com.eclipsesuites.dto.response.BoletaReservaDTO;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.dto.response.ReservaResumenDTO;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IClientePerfilService;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({ClientePerfilController.class, ClientePerfilPageController.class})
@Import(SecurityConfig.class)
class ClientePerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClientePerfilService clientePerfilService;

    @MockitoBean
    private IClienteAuthService clienteAuthService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @Test
    void paginaDePerfilRequiereSesionDeCliente() throws Exception {
        mockMvc.perform(get("/cliente/perfil"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void paginaDePerfilMuestraSeccionesYNombreReal() throws Exception {
        when(clientePerfilService.obtenerPerfil("ana@example.com")).thenReturn(perfil());
        when(clientePerfilService.listarReservas("ana@example.com")).thenReturn(List.of(reserva()));

        mockMvc.perform(get("/cliente/perfil")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/perfil"))
                .andExpect(content().string(containsString("Perfil de usuario")))
                .andExpect(content().string(containsString("Ana Rojas")))
                .andExpect(content().string(containsString("Historial de reservas")))
                .andExpect(content().string(containsString("Configurar datos")))
                .andExpect(content().string(containsString("Eliminar cuenta")))
                .andExpect(content().string(containsString("Suite Galaxia")))
                .andExpect(content().string(containsString("Ver boleta")));
    }

    @Test
    void paginaDePerfilSinReservasMuestraEstadoVacio() throws Exception {
        when(clientePerfilService.obtenerPerfil("ana@example.com")).thenReturn(perfil());
        when(clientePerfilService.listarReservas("ana@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/cliente/perfil")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No tienes ninguna reserva realizada")));
    }

    @Test
    void apiDePerfilDevuelveJsonDelCliente() throws Exception {
        when(clientePerfilService.obtenerPerfil("ana@example.com")).thenReturn(perfil());

        mockMvc.perform(get("/api/auth/client/perfil")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Rojas"))
                .andExpect(jsonPath("$.correoElectronico").value("ana@example.com"))
                .andExpect(jsonPath("$.telefono").value("+51999123456"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.totalReservas").value(1));
    }

    @Test
    void apiDePerfilSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/auth/client/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDePerfilNoEsAccesibleParaEmpleados() throws Exception {
        mockMvc.perform(get("/api/auth/client/perfil")
                        .with(user("recepcion").roles("RECEPCION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarPerfilAceptaDatosValidos() throws Exception {
        when(clientePerfilService.obtenerPerfil("ana@example.com")).thenReturn(perfil());

        mockMvc.perform(put("/api/auth/client/perfil")
                        .with(user("ana@example.com").roles("CLIENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Rojas Quispe\","
                                + "\"telefono\":\"+51999123456\",\"edad\":31}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Ana Rojas"));

        verify(clienteAuthService).actualizarPerfil(eq("ana@example.com"),
                any(PerfilClienteUpdateDTO.class));
    }

    @Test
    void actualizarPerfilSinTokenCsrfSeRechaza() throws Exception {
        mockMvc.perform(put("/api/auth/client/perfil")
                        .with(user("ana@example.com").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Rojas Quispe\","
                                + "\"telefono\":\"+51999123456\",\"edad\":31}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarPerfilValidaTelefonoInternacional() throws Exception {
        mockMvc.perform(put("/api/auth/client/perfil")
                        .with(user("ana@example.com").roles("CLIENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Rojas Quispe\","
                                + "\"telefono\":\"999123456\",\"edad\":31}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.telefono").value(
                        containsString("E.164")));
    }

    @Test
    void apiDeReservasDevuelveElHistorial() throws Exception {
        when(clientePerfilService.listarReservas("ana@example.com"))
                .thenReturn(List.of(reserva()));

        mockMvc.perform(get("/api/auth/client/reservas")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].suite").value("Suite Galaxia"))
                .andExpect(jsonPath("$[0].estadoEtiqueta").value("Confirmada"))
                .andExpect(jsonPath("$[0].imagen").value("/img/galaxia.webp"))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    @Test
    void apiDeBoletaDevuelveElDesglose() throws Exception {
        when(clientePerfilService.obtenerBoleta("ana@example.com", 17L)).thenReturn(boleta());

        mockMvc.perform(get("/api/auth/client/reservas/17/boleta")
                        .with(user("ana@example.com").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroBoleta").value("001-0000017"))
                .andExpect(jsonPath("$.subtotal").value(100.00))
                .andExpect(jsonPath("$.igv").value(18.00))
                .andExpect(jsonPath("$.total").value(118.00));
    }

    @Test
    void eliminarCuentaRespondeMensajeYDesactiva() throws Exception {
        mockMvc.perform(delete("/api/auth/client/cuenta")
                        .with(user("ana@example.com").roles("CLIENTE"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("Tu cuenta fue eliminada correctamente"));

        verify(clienteAuthService).eliminarCuenta("ana@example.com");
    }

    private PerfilClienteResponseDTO perfil() {
        return new PerfilClienteResponseDTO(
                1, "Ana", "Rojas", "Ana Rojas", "ana@example.com",
                "DNI", "12345678", "PER", "+51999123456", 30, "CLIENTE",
                "15/01/2026 10:00", true, reserva(), 1);
    }

    private ReservaResumenDTO reserva() {
        return new ReservaResumenDTO(
                17L, "Reserva #17", "Suite Galaxia", "Espacial", "509",
                "CONFIRMADA", "Confirmada", "15/06/2099 18:00", "15/06/2099 22:00",
                "4 horas", 2, "Yape", new BigDecimal("118.00"),
                "10/06/2099 09:30", "/img/galaxia.webp", true);
    }

    private BoletaReservaDTO boleta() {
        return new BoletaReservaDTO(
                17L, "001-0000017", "ECLIPSE SUITES HOTEL", "Ana Rojas",
                "DNI", "12345678", "ana@example.com", "Suite Galaxia", "509",
                "4 horas", "15/06/2099 18:00", "15/06/2099 22:00", "Confirmada",
                "Yape", 2, new BigDecimal("100.00"), new BigDecimal("18.00"),
                new BigDecimal("118.00"), "10/06/2099 09:30");
    }
}
