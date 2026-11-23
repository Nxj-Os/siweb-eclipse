package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Límite de mensajes por ventana deslizante de un minuto. Es local a cada
 * instancia, igual que {@link DisponibilidadRateLimiter}.
 */
@Component
public class ChatbotRateLimiter {

    private static final long DURACION_VENTANA_SEGUNDOS = 60;
    private static final int MAXIMO_DIRECCIONES = 10_000;

    private final ConcurrentMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final Clock reloj = Clock.systemUTC();

    public boolean permitir(String clave, int maximoPorVentana) {
        if (clave == null || clave.isBlank() || maximoPorVentana <= 0) {
            return false;
        }
        Instant ahora = reloj.instant();
        ventanas.entrySet().removeIf(entry ->
                entry.getValue().inicio.plusSeconds(DURACION_VENTANA_SEGUNDOS).isBefore(ahora));
        if (ventanas.size() >= MAXIMO_DIRECCIONES && !ventanas.containsKey(clave)) {
            return false;
        }

        Ventana ventana = ventanas.compute(clave, (ignorado, actual) -> {
            if (actual == null
                    || actual.inicio.plusSeconds(DURACION_VENTANA_SEGUNDOS).isBefore(ahora)) {
                return new Ventana(ahora, 1);
            }
            return new Ventana(actual.inicio, actual.solicitudes + 1);
        });
        return ventana.solicitudes <= maximoPorVentana;
    }

    public void reiniciar(String clave) {
        if (clave != null) {
            ventanas.remove(clave);
        }
    }

    private record Ventana(Instant inicio, int solicitudes) {
    }
}
