package pe.com.eclipsesuites.service.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.com.eclipsesuites.config.ChatbotProperties;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente mínimo de la API generateContent de Gemini. Usa RestClient de Spring
 * para no añadir dependencias del SDK de Google.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final Pattern MENSAJE_ERROR =
            Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient restClient;
    private final ChatbotProperties propiedades;

    public GeminiClient(ChatbotProperties propiedades) {
        this.propiedades = propiedades;

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(propiedades.getTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofMillis(propiedades.getTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(propiedades.getBaseUrl())
                .defaultHeader("x-goog-api-key", propiedades.getApiKey())
                .requestFactory(fabrica)
                .build();
    }

    /**
     * Envía el historial a Gemini y devuelve la respuesta en texto plano.
     *
     * @throws GeminiNoDisponibleException si la clave falta, la API responde un
     *                                     error o no hay conexión.
     */
    public String generar(String instruccionSistema, List<GeminiTurno> turnos) {
        if (!propiedades.tieneClave()) {
            throw new GeminiNoDisponibleException("Asistente IA no configurado");
        }

        SolicitudChat solicitud = new SolicitudChat(
                new InstruccionSistema(List.of(new Parte(instruccionSistema))),
                turnos.stream()
                        .map(turno -> new Contenido(turno.rol(), List.of(new Parte(turno.texto()))))
                        .toList(),
                new ConfigGeneracion(0.6, 600));

        try {
            RespuestaChat datos = restClient.post()
                    .uri("/v1beta/models/{modelo}:generateContent", propiedades.getModelo())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .body(RespuestaChat.class);
            return extraerTexto(datos);
        } catch (RestClientResponseException e) {
            throw traducirError(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.warn("Sin respuesta de Gemini: {}", e.getMessage());
            throw new GeminiNoDisponibleException("Sin conexión con el asistente", e);
        } catch (GeminiNoDisponibleException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Fallo inesperado al consultar Gemini: {}", e.getMessage());
            throw new GeminiNoDisponibleException("No se pudo consultar el asistente", e);
        }
    }

    private String extraerTexto(RespuestaChat datos) {
        if (datos == null || datos.candidates() == null || datos.candidates().isEmpty()) {
            throw new GeminiNoDisponibleException("El asistente no devolvió contenido");
        }
        Contenido contenido = datos.candidates().get(0).content();
        if (contenido == null || contenido.parts() == null || contenido.parts().isEmpty()) {
            throw new GeminiNoDisponibleException("El asistente no devolvió contenido");
        }
        String texto = contenido.parts().stream()
                .map(Parte::text)
                .filter(parcial -> parcial != null && !parcial.isBlank())
                .findFirst()
                .orElseThrow(() ->
                        new GeminiNoDisponibleException("El asistente no devolvió contenido"));
        return texto.trim();
    }

    private RuntimeException traducirError(int status, String cuerpo) {
        String mensajeApi = extraerMensajeError(cuerpo);
        if (status == 401 || status == 403) {
            log.error("Gemini rechazó la clave de API (HTTP {})", status);
            return new GeminiNoDisponibleException("Clave de Gemini no válida");
        }
        if (status == 429) {
            log.warn("Gemini limitó las consultas: {}", mensajeApi);
            return new GeminiNoDisponibleException(
                    "El asistente está saturado, vuelve en unos minutos");
        }
        if (status == 400) {
            log.warn("Solicitud rechazada por Gemini: {}", mensajeApi);
            return new GeminiNoDisponibleException("No se pudo procesar la consulta");
        }
        log.error("Gemini respondió HTTP {}: {}", status, mensajeApi);
        return new GeminiNoDisponibleException("El asistente no está disponible ahora");
    }

    private String extraerMensajeError(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin detalle";
        }
        Matcher coincidencia = MENSAJE_ERROR.matcher(cuerpo);
        return coincidencia.find() ? coincidencia.group(1) : cuerpo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Contenido(String role, List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstruccionSistema(List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConfigGeneracion(Double temperature, Integer maxOutputTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SolicitudChat(InstruccionSistema systemInstruction,
                         List<Contenido> contents,
                         ConfigGeneracion generationConfig) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Contenido content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaChat(List<Candidato> candidates) {
    }
}
