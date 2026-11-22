package pe.com.eclipsesuites.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatbotProperties {

    private final String baseUrl;
    private final String apiKey;
    private final String modelo;
    private final long timeoutMs;
    private final int maxHistorial;
    private final int maxEntrada;
    private final int limitePublicoMin;
    private final int limiteClienteMin;
    private final int maxMensajesAnonimos;

    public ChatbotProperties(
            @Value("${app.chat.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.chat.api-key:}") String apiKey,
            @Value("${app.chat.modelo:gemini-2.5-flash}") String modelo,
            @Value("${app.chat.timeout-ms:20000}") long timeoutMs,
            @Value("${app.chat.max-historial:12}") int maxHistorial,
            @Value("${app.chat.max-entrada:600}") int maxEntrada,
            @Value("${app.chat.limite-publico-min:10}") int limitePublicoMin,
            @Value("${app.chat.limite-cliente-min:30}") int limiteClienteMin,
            @Value("${app.chat.max-mensajes-anonimos:12}") int maxMensajesAnonimos) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo == null ? "" : modelo.trim();
        this.timeoutMs = timeoutMs;
        this.maxHistorial = maxHistorial;
        this.maxEntrada = maxEntrada;
        this.limitePublicoMin = limitePublicoMin;
        this.limiteClienteMin = limiteClienteMin;
        this.maxMensajesAnonimos = maxMensajesAnonimos;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelo() {
        return modelo;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxHistorial() {
        return maxHistorial;
    }

    public int getMaxEntrada() {
        return maxEntrada;
    }

    public int getLimitePublicoMin() {
        return limitePublicoMin;
    }

    public int getLimiteClienteMin() {
        return limiteClienteMin;
    }

    public int getMaxMensajesAnonimos() {
        return maxMensajesAnonimos;
    }

    public boolean tieneClave() {
        return !apiKey.isEmpty() && !modelo.isEmpty();
    }
}
