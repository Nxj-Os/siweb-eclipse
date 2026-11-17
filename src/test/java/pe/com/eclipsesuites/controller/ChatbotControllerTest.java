package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatEstadoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatMensajeResponseDTO;
import pe.com.eclipsesuites.service.IChatbotService;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
@Import(SecurityConfig.class)
class ChatbotControllerTest {

    private static final String MENSAJE_JSON = "{\"mensaje\":\"Hola\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IChatbotService chatbotService;

    @MockitoBean
    private IClienteAuthService clienteAuthService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @Test
    void catalogoEsPublicoYSinSesion() throws Exception {
        when(chatbotService.catalogo()).thenReturn(
                new CatalogoChatbotResponseDTO(List.of(), 0, 0, "2026-01-01"));

        mockMvc.perform(get("/api/chatbot/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void enviarMensajeExigeTokenCsrf() throws Exception {
        mockMvc.perform(post("/api/chatbot/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_JSON))
                .andExpect(status().isForbidden());

        verify(chatbotService, org.mockito.Mockito.never())
                .enviar(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void enviarMensajeFuncionaComoVisitanteSinSesion() throws Exception {
        when(chatbotService.enviar(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new ChatMensajeResponseDTO(
                        "Hola, soy el asistente de Eclipse Suites.",
                        "SALUDO", false, "SIN_COLA", 0,
                        List.of("¿Cuánto cuesta una suite?"), "PUBLICO", 1, 12));

        mockMvc.perform(post("/api/chatbot/mensaje").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("PUBLICO"))
                .andExpect(jsonPath("$.intencion").value("SALUDO"))
                .andExpect(jsonPath("$.mensajesMaximos").value(12));

        verify(chatbotService).enviar(anyString(), eq(null), anyString(), anyString(),
                eq("Hola"));
    }

    @Test
    void historicoYEstadoRespondenComoVisitante() throws Exception {
        when(chatbotService.historico(anyString(), any()))
                .thenReturn(new pe.com.eclipsesuites.dto.response.ChatHistoricoResponseDTO(
                        List.of(), "SIN_COLA", 0, "PUBLICO"));
        when(chatbotService.estado(anyString(), any()))
                .thenReturn(new ChatEstadoResponseDTO(
                        "SIN_COLA", 0, "PUBLICO", 0, 12, false));

        mockMvc.perform(get("/api/chatbot/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("PUBLICO"));

        mockMvc.perform(get("/api/chatbot/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajesMaximos").value(12));
    }

    @Test
    void reiniciarLaColaRequiereCsrf() throws Exception {
        when(chatbotService.estado(anyString(), any()))
                .thenReturn(new ChatEstadoResponseDTO(
                        "SIN_COLA", 0, "PUBLICO", 0, 12, false));

        mockMvc.perform(post("/api/chatbot/reiniciar"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/chatbot/reiniciar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCola").value("SIN_COLA"));
    }
}
