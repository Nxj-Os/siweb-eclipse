package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.entity.RolEmpleado;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN_JSON_VALIDO =
            "{\"username\":\"admin\",\"password\":\"Admin@123\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    private Empleado empleadoAdmin() {
        RolEmpleado rol = RolEmpleado.builder()
                .id(1)
                .nombreRol("ADMIN")
                .permisosSistema("REPORTES,RACK")
                .build();
        return Empleado.builder()
                .id(1)
                .rol(rol)
                .nombre("Nilton")
                .apellido("Quispe")
                .usernameLogin("admin")
                .passwordHashBcrypt(new BCryptPasswordEncoder(10).encode("Admin@123"))
                .turnoLaboral("DIURNO")
                .estadoActivo(true)
                .build();
    }

    private User userDetailsAdmin() {
        Set<String> authorities = Set.of("ROLE_ADMIN", "PERMISO_REPORTES", "PERMISO_RACK");
        return (User) User.builder()
                .username("admin")
                .password("hash")
                .authorities(authorities.stream()
                        .map(a -> new org.springframework.security.core.authority.SimpleGrantedAuthority(a))
                        .collect(Collectors.toList()))
                .build();
    }

    @Test
    void login_camposVacios_retorna400ConMensajesDeValidacion() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.username").value("El username es obligatorio"))
                .andExpect(jsonPath("$.detalles.password").value("El password es obligatorio"));

        verifyNoInteractions(empleadoAuthService);
    }

    @Test
    void login_credencialesValidas_retorna200ConRolYPermisos() throws Exception {
        Empleado empleado = empleadoAdmin();
        when(empleadoAuthService.autenticar("admin", "Admin@123")).thenReturn(empleado);
        when(empleadoAuthService.loadUserByUsername("admin")).thenReturn(userDetailsAdmin());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nombreCompleto").value("Nilton Quispe"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.permisos", hasItem("REPORTES")))
                .andExpect(jsonPath("$.permisos", hasItem("RACK")))
                .andExpect(jsonPath("$.mensaje").value("Login exitoso"));
    }

    @Test
    void login_passwordIncorrecta_retorna401() throws Exception {
        when(empleadoAuthService.autenticar("admin", "Admin@123"))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void login_usuarioInactivo_retorna401ConMensajeDeCuentaInactiva() throws Exception {
        when(empleadoAuthService.autenticar("admin", "Admin@123"))
                .thenThrow(new DisabledException("La cuenta del empleado está inactiva"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(containsString("inactiva")));
    }

    @Test
    void login_usuarioInexistente_retorna401SinFiltrarExistencia() throws Exception {
        when(empleadoAuthService.autenticar("admin", "Admin@123"))
                .thenThrow(new UsernameNotFoundException("No existe empleado con username: admin"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void me_sinSesion_retorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void me_conSesion_retornaDatosDelEmpleado() throws Exception {
        when(empleadoAuthService.buscarPorUsername("admin")).thenReturn(empleadoAdmin());

        mockMvc.perform(get("/api/auth/me").with(user(userDetailsAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void logout_conSesion_retorna200() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(user(userDetailsAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada"));
    }

    @Test
    void login_sinTokenCsrf_funcionaPorSerRutaApi() throws Exception {
        Empleado empleado = empleadoAdmin();
        when(empleadoAuthService.autenticar("admin", "Admin@123")).thenReturn(empleado);
        when(empleadoAuthService.loadUserByUsername("admin")).thenReturn(userDetailsAdmin());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON_VALIDO))
                .andExpect(status().isOk());
    }
}
