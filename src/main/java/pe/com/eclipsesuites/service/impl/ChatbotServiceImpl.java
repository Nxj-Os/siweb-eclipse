package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.config.ChatbotProperties;
import pe.com.eclipsesuites.dao.ConversacionChatbotRepository;
import pe.com.eclipsesuites.dao.MensajeChatbotRepository;
import pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatEstadoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatHistoricoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatMensajeHistoricoDTO;
import pe.com.eclipsesuites.dto.response.ChatMensajeResponseDTO;
import pe.com.eclipsesuites.entity.ConversacionChatbot;
import pe.com.eclipsesuites.entity.MensajeChatbot;
import pe.com.eclipsesuites.service.IChatbotService;
import pe.com.eclipsesuites.service.gemini.GeminiClient;
import pe.com.eclipsesuites.service.gemini.GeminiNoDisponibleException;
import pe.com.eclipsesuites.service.gemini.GeminiTurno;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements IChatbotService {

    public static final String ROL_USUARIO = "usuario";
    public static final String ROL_ASISTENTE = "asistente";

    public static final String MODO_PUBLICO = "PUBLICO";
    public static final String MODO_CLIENTE = "CLIENTE";
    public static final String MODO_CLIENTE_FALLBACK = "CLIENTE_FALLBACK";

    public static final String COLA_SIN_COLA = "SIN_COLA";
    public static final String COLA_EN_COLA = "EN_COLA";
    public static final String COLA_ESPERANDO = "ESPERANDO";

    static final String LIMITE_POR_VENTANA =
            "Estás enviando muchas consultas. Espera un minuto y vuelve a intentarlo.";
    static final String LIMITE_ANONIMO =
            "Alcanzaste el límite de consultas de esta visita. Crea una cuenta en /registro "
                    + "para seguir conversando con el asistente sin límites.";

    private final ConversacionChatbotRepository conversacionRepository;
    private final MensajeChatbotRepository mensajeRepository;
    private final ChatContextoBuilder contextoBuilder;
    private final RespuestasPredefinidas respuestasPredefinidas;
    private final IntencionChatbotDetector intencionDetector;
    private final ChatbotRateLimiter rateLimiter;
    private final ColaDerivacionStore colaDerivacion;
    private final GeminiClient geminiClient;
    private final ChatbotProperties propiedades;

    @Override
    @Transactional(readOnly = true)
    public CatalogoChatbotResponseDTO catalogo() {
        return contextoBuilder.catalogo();
    }

    @Override
    @Transactional
    public ChatMensajeResponseDTO enviar(String idSesion, Integer idCliente, String direccionIp,
                                         String canal, String mensaje) {
        String texto = recortar(mensaje);
        boolean esCliente = idCliente != null;
        String modo = esCliente ? MODO_CLIENTE : MODO_PUBLICO;
        String claveLimite = esCliente ? "cliente:" + idCliente : "ip:" + direccionIp;
        int limite = esCliente ? propiedades.getLimiteClienteMin()
                : propiedades.getLimitePublicoMin();
        if (!rateLimiter.permitir(claveLimite, limite)) {
            throw new ChatbotLimiteExcedidoException(LIMITE_POR_VENTANA);
        }

        ConversacionChatbot conversacion = obtenerOCrear(idSesion, idCliente, canal);
        long enviados = mensajeRepository.countByIdConversacionAndRolEmisor(
                conversacion.getId(), ROL_USUARIO);
        int maximos = esCliente ? 0 : propiedades.getMaxMensajesAnonimos();
        if (!esCliente && enviados >= propiedades.getMaxMensajesAnonimos()) {
            throw new ChatbotLimiteExcedidoException(LIMITE_ANONIMO);
        }

        Intencion intencion = intencionDetector.detectar(texto);
        guardar(conversacion.getId(), ROL_USUARIO, texto, intencion, OffsetDateTime.now());

        if (intencion == Intencion.DERIVAR_RECEPCION) {
            derivar(conversacion, idSesion);
        }

        String respuesta;
        String modoFinal = modo;
        if (!esCliente) {
            respuesta = respuestasPredefinidas.responder(intencion);
        } else {
            try {
                respuesta = geminiClient.generar(contextoBuilder.instruccionSistema(),
                        historialGemini(conversacion.getId()));
                modoFinal = MODO_CLIENTE;
            } catch (GeminiNoDisponibleException ex) {
                log.warn("Gemini no disponible, se usa respuesta predefinida: {}",
                        ex.getMessage());
                respuesta = respuestasPredefinidas.responderParaCliente(intencion);
                modoFinal = MODO_CLIENTE_FALLBACK;
            }
        }

        guardar(conversacion.getId(), ROL_ASISTENTE, respuesta, intencion, OffsetDateTime.now());

        return new ChatMensajeResponseDTO(
                respuesta,
                intencion.name(),
                colaDerivacion.enCola(idSesion),
                estadoCola(idSesion),
                colaDerivacion.minutosRestantes(idSesion),
                RespuestasPredefinidas.sugerenciasDe(intencion),
                modoFinal,
                (int) enviados + 1,
                maximos);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatHistoricoResponseDTO historico(String idSesion, Integer idCliente) {
        List<ChatMensajeHistoricoDTO> mensajes = conversacionRepository.findByIdSesionWeb(idSesion)
                .map(conversacion -> mensajeRepository
                        .findByConversacion(conversacion.getId()).stream()
                        .map(mensaje -> new ChatMensajeHistoricoDTO(
                                mensaje.getRolEmisor(),
                                mensaje.getContenidoMensaje(),
                                mensaje.getFhMensaje(),
                                mensaje.getIntencionDetectada()))
                        .toList())
                .orElse(List.of());
        return new ChatHistoricoResponseDTO(
                mensajes,
                estadoCola(idSesion),
                colaDerivacion.minutosRestantes(idSesion),
                modo(idCliente));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatEstadoResponseDTO estado(String idSesion, Integer idCliente) {
        boolean esCliente = idCliente != null;
        ConversacionChatbot conversacion = conversacionRepository.findByIdSesionWeb(idSesion)
                .orElse(null);
        long enviados = conversacion == null ? 0
                : mensajeRepository.countByIdConversacionAndRolEmisor(
                        conversacion.getId(), ROL_USUARIO);
        return new ChatEstadoResponseDTO(
                estadoCola(idSesion),
                colaDerivacion.minutosRestantes(idSesion),
                modo(idCliente),
                (int) enviados,
                esCliente ? 0 : propiedades.getMaxMensajesAnonimos(),
                colaDerivacion.enCola(idSesion));
    }

    @Override
    public void reiniciarCola(String idSesion) {
        colaDerivacion.salir(idSesion);
    }

    private void derivar(ConversacionChatbot conversacion, String idSesion) {
        if (!Boolean.TRUE.equals(conversacion.getDerivoAReserva())) {
            conversacion.setDerivoAReserva(true);
            conversacionRepository.save(conversacion);
        }
        if (!colaDerivacion.enCola(idSesion)) {
            colaDerivacion.entrar(idSesion);
        }
    }

    private ConversacionChatbot obtenerOCrear(String idSesion, Integer idCliente, String canal) {
        ConversacionChatbot conversacion = conversacionRepository.findByIdSesionWeb(idSesion)
                .orElseGet(() -> conversacionRepository.save(ConversacionChatbot.builder()
                        .idCliente(idCliente)
                        .idSesionWeb(idSesion)
                        .canalDispositivo(StringUtils.isBlank(canal) ? "WEB" : canal)
                        .derivoAReserva(false)
                        .build()));
        if (idCliente != null && conversacion.getIdCliente() == null) {
            conversacion.setIdCliente(idCliente);
            return conversacionRepository.save(conversacion);
        }
        return conversacion;
    }

    private void guardar(Long idConversacion, String rol, String contenido, Intencion intencion,
                         OffsetDateTime cuando) {
        mensajeRepository.save(MensajeChatbot.builder()
                .idConversacion(idConversacion)
                .rolEmisor(rol)
                .contenidoMensaje(contenido)
                .intencionDetectada(intencion.name())
                .fhMensaje(cuando)
                .build());
    }

    private List<GeminiTurno> historialGemini(Long idConversacion) {
        List<MensajeChatbot> mensajes = mensajeRepository.findByConversacion(idConversacion);
        int desde = Math.max(0, mensajes.size() - propiedades.getMaxHistorial());
        while (desde < mensajes.size()
                && !ROL_USUARIO.equals(mensajes.get(desde).getRolEmisor())) {
            desde++;
        }
        return mensajes.subList(desde, mensajes.size()).stream()
                .map(mensaje -> ROL_USUARIO.equals(mensaje.getRolEmisor())
                        ? GeminiTurno.usuario(mensaje.getContenidoMensaje())
                        : GeminiTurno.modelo(mensaje.getContenidoMensaje()))
                .toList();
    }

    private String estadoCola(String idSesion) {
        if (!colaDerivacion.enCola(idSesion)) {
            return COLA_SIN_COLA;
        }
        return colaDerivacion.minutosRestantes(idSesion) > 0 ? COLA_EN_COLA : COLA_ESPERANDO;
    }

    private static String modo(Integer idCliente) {
        return idCliente != null ? MODO_CLIENTE : MODO_PUBLICO;
    }

    private String recortar(String mensaje) {
        String texto = StringUtils.trimToEmpty(mensaje);
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Escribe una consulta antes de enviarla");
        }
        int maximo = propiedades.getMaxEntrada();
        if (texto.length() > maximo) {
            texto = texto.substring(0, maximo);
        }
        return texto;
    }
}
