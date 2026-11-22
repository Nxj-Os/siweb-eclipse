package pe.com.eclipsesuites;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siweb_chatbot;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.profiles.active=demo"
})
@AutoConfigureMockMvc
class ChatbotApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginaDelAsistenteEsPublica() throws Exception {
        mockMvc.perform(get("/asistente"))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue(
                        result.getResponse().getContentAsString().contains("Asistente Eclipse")));
    }

    @Test
    void catalogoRespondeConSuitesSembradas() throws Exception {
        mockMvc.perform(get("/api/chatbot/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(16))
                .andExpect(jsonPath("$.disponibles").value(16))
                .andExpect(jsonPath("$.habitaciones[3].numHabitacion").value("302"))
                .andExpect(jsonPath("$.habitaciones[3].disponible").value(true));
    }

    @Test
    void enviarMensajeSinCsrfQuedaProhibido() throws Exception {
        mockMvc.perform(post("/api/chatbot/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteConversaSinSesionYEmiteCookieDeChat() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/chatbot/mensaje")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Hola, ¿cuánto cuesta una suite?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("PUBLICO"))
                .andExpect(jsonPath("$.intencion").value("TARIFA"))
                .andExpect(jsonPath("$.respuesta").isNotEmpty())
                .andExpect(jsonPath("$.mensajesMaximos").value(12))
                .andReturn();

        MockHttpServletResponse respuesta = resultado.getResponse();
        assertNotNull(respuesta.getCookie("ECLIPSE_CHAT_SID"),
                "El chat debe emitir su cookie de sesión propia");
    }

    @Test
    void derivacionARecepcionActivaElEstadoDeCola() throws Exception {
        mockMvc.perform(post("/api/chatbot/mensaje")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.20");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Quiero hablar con recepción\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intencion").value("DERIVAR_RECEPCION"))
                .andExpect(jsonPath("$.derivado").value(true))
                .andExpect(jsonPath("$.estadoCola").value("EN_COLA"))
                .andExpect(jsonPath("$.minutosRestantes").value(2));
    }

    @Test
    void historicoEmpiezaVacioParaUnaVisitaNueva() throws Exception {
        mockMvc.perform(get("/api/chatbot/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("PUBLICO"))
                .andExpect(jsonPath("$.mensajes").isArray());
    }

    @Test
    void elModoPublicoExcedeSuLimitePorMinuto() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/chatbot/mensaje")
                            .with(csrf())
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.44");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mensaje\":\"hola " + i + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/chatbot/mensaje")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.44");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola otra vez\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }
}
