package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthPageController.class)
@Import(SecurityConfig.class)
class AuthPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @Test
    void loginCliente_renderizaFormularioDeCorreo() throws Exception {
        mockMvc.perform(get("/login").param("tipo", "cliente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Correo electrónico")))
                .andExpect(content().string(containsString("Crear una cuenta")));
    }

    @Test
    void loginEmpleado_renderizaFormularioDeUsuario() throws Exception {
        mockMvc.perform(get("/login").param("tipo", "empleado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario")))
                .andExpect(content().string(containsString("Accede con tus credenciales de empleado.")));
    }

    @Test
    void registro_renderizaFormularioCompleto() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crea tu cuenta")))
                .andExpect(content().string(containsString("confirmPassword")));
    }
}