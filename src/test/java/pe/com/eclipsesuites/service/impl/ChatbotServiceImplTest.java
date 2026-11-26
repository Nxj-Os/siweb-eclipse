package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.eclipsesuites.config.ChatbotProperties;
import pe.com.eclipsesuites.dao.ConversacionChatbotRepository;
import pe.com.eclipsesuites.dao.MensajeChatbotRepository;
import pe.com.eclipsesuites.dto.response.ChatMensajeResponseDTO;
import pe.com.eclipsesuites.entity.ConversacionChatbot;
import pe.com.eclipsesuites.entity.MensajeChatbot;
import pe.com.eclipsesuites.service.gemini.GeminiClient;
import pe.com.eclipsesuites.service.gemini.GeminiNoDisponibleException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {

    private static final String SESION = "sesion-de-prueba";

    @Mock
    private ConversacionChatbotRepository conversacionRepository;

    @Mock
    private MensajeChatbotRepository mensajeRepository;

    @Mock
    private ChatContextoBuilder contextoBuilder;

    @Mock
    private GeminiClient geminiClient;

    private ChatbotServiceImpl servicio;

    @BeforeEach
    void preparar() {
        ChatbotProperties propiedades = new ChatbotProperties(
                "https://generativelanguage.test", "clave-de-prueba", "modelo-de-prueba",
                1000, 12, 600, 10, 30, 12);
        RespuestasPredefinidas respuestas = new RespuestasPredefinidas(contextoBuilder);
        servicio = new ChatbotServiceImpl(
                conversacionRepository,
                mensajeRepository,
                contextoBuilder,
                respuestas,
                new IntencionChatbotDetector(),
                new ChatbotRateLimiter(),
                new ColaDerivacionStore(),
                geminiClient,
                propiedades);

        lenient().when(conversacionRepository.findByIdSesionWeb(anyString()))
                .thenReturn(Optional.of(conversacion()));
        lenient().when(conversacionRepository.save(any(ConversacionChatbot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mensajeRepository.save(any(MensajeChatbot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mensajeRepository.countByIdConversacionAndRolEmisor(anyLong(), anyString()))
                .thenReturn(0L);
        lenient().when(mensajeRepository.findByConversacion(anyLong())).thenReturn(List.of());
        lenient().when(contextoBuilder.tarifasPorTipo())
                .thenReturn(Map.of("Suite VIP Deluxe", new BigDecimal("80.00")));
        lenient().when(contextoBuilder.catalogo()).thenReturn(catalogoVacio());
        lenient().when(contextoBuilder.instruccionSistema())
                .thenReturn("Eres el asistente de Eclipse Suites.");
    }

    @Test
    void modoPublicoRespondeConDatosDelHotelSinInvocarGemini() {
        ChatMensajeResponseDTO respuesta = servicio.enviar(
                SESION, null, "192.0.2.10", "WEB", "¿Cuánto cuesta una suite?");

        assertEquals(ChatbotServiceImpl.MODO_PUBLICO, respuesta.modo());
        assertEquals(Intencion.TARIFA.name(), respuesta.intencion());
        assertNotNull(respuesta.respuesta());
        assertFalse(respuesta.respuesta().isBlank());
        verifyNoInteractions(geminiClient);
        verify(mensajeRepository, times(2)).save(any(MensajeChatbot.class));
    }

    @Test
    void modoPublicoSeQuedaEnElLimiteDeMensajesDeLaVisita() {
        lenient().when(mensajeRepository.countByIdConversacionAndRolEmisor(
                anyLong(), eq(ChatbotServiceImpl.ROL_USUARIO))).thenReturn(12L);

        ChatbotLimiteExcedidoException error = assertThrows(
                ChatbotLimiteExcedidoException.class,
                () -> servicio.enviar(SESION, null, "192.0.2.11", "WEB", "hola"));

        assertEquals(ChatbotServiceImpl.LIMITE_ANONIMO, error.getMessage());
        verifyNoInteractions(geminiClient);
    }

    @Test
    void clienteLogueadoUsaGemini() {
        when(geminiClient.generar(anyString(), anyList())).thenReturn("Respuesta del modelo");

        ChatMensajeResponseDTO respuesta = servicio.enviar(
                SESION, 7, "192.0.2.12", "WEB", "Cuéntame sobre las suites");

        assertEquals(ChatbotServiceImpl.MODO_CLIENTE, respuesta.modo());
        assertEquals("Respuesta del modelo", respuesta.respuesta());
        assertTrue(respuesta.respuesta().length() > 0);
    }

    @Test
    void clienteCaeARespuestaPredefinidaCuandoGeminiFalla() {
        when(geminiClient.generar(anyString(), anyList()))
                .thenThrow(new GeminiNoDisponibleException("Asistente IA no configurado"));

        ChatMensajeResponseDTO respuesta = servicio.enviar(
                SESION, 7, "192.0.2.13", "WEB", "Hola");

        assertEquals(ChatbotServiceImpl.MODO_CLIENTE_FALLBACK, respuesta.modo());
        assertNotNull(respuesta.respuesta());
        assertFalse(respuesta.respuesta().isBlank());
        assertFalse(respuesta.respuesta().contains("/registro"));
    }

    @Test
    void derivarAMarcaLaConversacionYActivaLaCola() {
        ChatMensajeResponseDTO respuesta = servicio.enviar(
                SESION, null, "192.0.2.14", "WEB", "Quiero hablar con recepción");

        assertEquals(Intencion.DERIVAR_RECEPCION.name(), respuesta.intencion());
        assertTrue(respuesta.derivado());
        assertEquals(ChatbotServiceImpl.COLA_EN_COLA, respuesta.estadoCola());
        assertEquals(ColaDerivacionStore.TIEMPO_ESTIMADO_MINUTOS,
                respuesta.minutosRestantes());

        ArgumentCaptor<ConversacionChatbot> captor =
                ArgumentCaptor.forClass(ConversacionChatbot.class);
        verify(conversacionRepository).save(captor.capture());
        assertTrue(captor.getValue().getDerivoAReserva());
    }

    @Test
    void limpiarLaColaDevuelveAlAsistente() {
        servicio.enviar(SESION, null, "192.0.2.15", "WEB",
                "Necesito hablar con recepción");
        assertTrue(servicio.estado(SESION, null).derivado());

        servicio.reiniciarCola(SESION);

        assertFalse(servicio.estado(SESION, null).derivado());
        assertEquals(ChatbotServiceImpl.COLA_SIN_COLA, servicio.estado(SESION, null).estadoCola());
    }

    @Test
    void excedeElLimitePorVentanaDelMinuto() {
        for (int i = 0; i < 10; i++) {
            servicio.enviar(SESION + i, null, "198.51.100.7", "WEB", "hola");
        }

        ChatbotLimiteExcedidoException error = assertThrows(
                ChatbotLimiteExcedidoException.class,
                () -> servicio.enviar(SESION + "once", null, "198.51.100.7", "WEB", "hola"));

        assertEquals(ChatbotServiceImpl.LIMITE_POR_VENTANA, error.getMessage());
    }

    @Test
    void mensajeVacioSeRechazaAntesDeEscribirEnBase() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.enviar(SESION, null, "192.0.2.16", "WEB", "   "));
        verifyNoInteractions(mensajeRepository);
    }

    private static ConversacionChatbot conversacion() {
        return ConversacionChatbot.builder()
                .id(1L)
                .idCliente(null)
                .idSesionWeb(SESION)
                .canalDispositivo("WEB")
                .derivoAReserva(false)
                .build();
    }

    private static pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO catalogoVacio() {
        return new pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO(
                List.of(), 0, 0, "2026-01-01");
    }
}
