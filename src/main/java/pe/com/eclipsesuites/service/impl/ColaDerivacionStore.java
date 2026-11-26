package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Estado de la derivación a recepción, en memoria. No hay panel de recepción
 * en esta fase: el estado es informativo para el chat y se pierde al reiniciar
 * la aplicación.
 */
@Component
public class ColaDerivacionStore {

    public static final int TIEMPO_ESTIMADO_MINUTOS = 2;
    private static final Duration VIDA_MAXIMA = Duration.ofMinutes(30);

    private final ConcurrentMap<String, Entrada> colas = new ConcurrentHashMap<>();
    private final Clock reloj = Clock.systemUTC();

    public void entrar(String idSesion) {
        if (idSesion == null || idSesion.isBlank()) {
            return;
        }
        limpiarExpiradas();
        colas.put(idSesion, new Entrada(reloj.instant()));
    }

    public void salir(String idSesion) {
        if (idSesion != null) {
            colas.remove(idSesion);
        }
    }

    public boolean enCola(String idSesion) {
        if (idSesion == null || idSesion.isBlank()) {
            return false;
        }
        Entrada entrada = colas.get(idSesion);
        if (entrada == null) {
            return false;
        }
        if (Duration.between(entrada.inicio, reloj.instant()).compareTo(VIDA_MAXIMA) > 0) {
            colas.remove(idSesion);
            return false;
        }
        return true;
    }

    /**
     * Minutos restantes del tiempo estimado; 0 si ya se superó la estimación.
     */
    public int minutosRestantes(String idSesion) {
        if (!enCola(idSesion)) {
            return 0;
        }
        long transcurridos = Duration.between(colas.get(idSesion).inicio, reloj.instant())
                .toSeconds();
        long restantes = (long) TIEMPO_ESTIMADO_MINUTOS * 60 - transcurridos;
        if (restantes <= 0) {
            return 0;
        }
        return (int) Math.ceil(restantes / 60.0);
    }

    public boolean superoEstimacion(String idSesion) {
        return enCola(idSesion) && minutosRestantes(idSesion) == 0;
    }

    private void limpiarExpiradas() {
        Instant ahora = reloj.instant();
        colas.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().inicio, ahora).compareTo(VIDA_MAXIMA) > 0);
    }

    private record Entrada(Instant inicio) {
    }
}
