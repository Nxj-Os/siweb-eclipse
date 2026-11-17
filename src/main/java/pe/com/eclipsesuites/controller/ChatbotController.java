package pe.com.eclipsesuites.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dto.request.ChatMensajeRequestDTO;
import pe.com.eclipsesuites.dto.response.CatalogoChatbotResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatEstadoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatHistoricoResponseDTO;
import pe.com.eclipsesuites.dto.response.ChatMensajeResponseDTO;
import pe.com.eclipsesuites.service.IChatbotService;
import pe.com.eclipsesuites.service.IClienteAuthService;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    public static final String COOKIE_SESION = "ECLIPSE_CHAT_SID";
    private static final String ROL_CLIENTE = "ROLE_CLIENTE";
    private static final Duration DURACION_COOKIE = Duration.ofDays(30);

    private final IChatbotService chatbotService;
    private final IClienteAuthService clienteAuthService;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSegura;

    @GetMapping("/catalogo")
    public CatalogoChatbotResponseDTO catalogo() {
        return chatbotService.catalogo();
    }

    @PostMapping("/mensaje")
    public ChatMensajeResponseDTO mensaje(@RequestBody ChatMensajeRequestDTO request,
                                          Authentication authentication,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        String idSesion = resolverSesion(httpRequest, httpResponse);
        return chatbotService.enviar(
                idSesion,
                idCliente(authentication),
                httpRequest.getRemoteAddr(),
                canal(httpRequest),
                request == null ? null : request.mensaje());
    }

    @GetMapping("/historico")
    public ChatHistoricoResponseDTO historico(Authentication authentication,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        String idSesion = resolverSesion(httpRequest, httpResponse);
        return chatbotService.historico(idSesion, idCliente(authentication));
    }

    @GetMapping("/estado")
    public ChatEstadoResponseDTO estado(Authentication authentication,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        String idSesion = resolverSesion(httpRequest, httpResponse);
        return chatbotService.estado(idSesion, idCliente(authentication));
    }

    @PostMapping("/reiniciar")
    public ChatEstadoResponseDTO reiniciar(Authentication authentication,
                                           HttpServletRequest httpRequest,
                                           HttpServletResponse httpResponse) {
        String idSesion = resolverSesion(httpRequest, httpResponse);
        chatbotService.reiniciarCola(idSesion);
        return chatbotService.estado(idSesion, idCliente(authentication));
    }

    /**
     * Identificador anónimo de la conversación. Si el visitante aún no tiene
     * cookie, se emite aquí para que la próxima petición ya la envíe.
     */
    private String resolverSesion(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_SESION.equals(cookie.getName())
                        && StringUtils.isNotBlank(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String nuevaSesion = UUID.randomUUID().toString().replace("-", "");
        ResponseCookie cookie = ResponseCookie.from(COOKIE_SESION, nuevaSesion)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .maxAge(DURACION_COOKIE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return nuevaSesion;
    }

    private Integer idCliente(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(autoridad -> ROL_CLIENTE.equals(autoridad.getAuthority()));
        if (!esCliente) {
            return null;
        }
        try {
            return clienteAuthService.buscarPorCorreo(authentication.getName()).getId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String canal(HttpServletRequest request) {
        String agente = StringUtils.defaultString(request.getHeader("User-Agent"))
                .toLowerCase(Locale.ROOT);
        boolean movil = agente.contains("mobile") || agente.contains("android")
                || agente.contains("iphone") || agente.contains("ipad");
        return movil ? "WEB_MOVIL" : "WEB";
    }
}
