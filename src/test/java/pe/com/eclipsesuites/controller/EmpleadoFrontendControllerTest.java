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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EmpleadoFrontendController.class)
@Import(SecurityConfig.class)
class EmpleadoFrontendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @Test
    void roomserviceUsaElPanelDeComandasDelEmpleado() throws Exception {
        mockMvc.perform(get("/empleado/roomservice")
                        .with(user("luis").roles("ROOMSERVICE")))
                .andExpect(status().isOk())
                .andExpect(view().name("roomservice/comandas"))
                .andExpect(content().string(containsString("comandas-tbody")))
                .andExpect(content().string(not(containsString("bottom-nav"))));
    }

    @Test
    void roomserviceExigeRolDeEmpleado() throws Exception {
        mockMvc.perform(get("/empleado/roomservice"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cartaDelHuespedNoSeSirveDesdeElDashboardDelEmpleado() throws Exception {
        mockMvc.perform(get("/empleado/roomservice")
                        .with(user("luis").roles("ROOMSERVICE")))
                .andExpect(content().string(not(containsString("mode-carrito"))))
                .andExpect(content().string(not(containsString("btn-solicitar"))));
    }
}
