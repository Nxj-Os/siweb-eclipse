package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DisponibilidadRateLimiter {

    private static final int MAXIMO_POR_VENTANA = 30;
    private static final long DURACION_VENTANA_SEGUNDOS = 60;
    private static final int MAXIMO_DIRECCIONES = 10_000;

    private final ConcurrentMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final Clock reloj = Clock.systemUTC();

    public boolean permitir(String direccionIp) {
        Instant ahora = reloj.instant();
        ventanas.entrySet().removeIf(entry ->
                entry.getValue().inicio.plusSeconds(DURACION_VENTANA_SEGUNDOS).isBefore(ahora));
        if (ventanas.size() >= MAXIMO_DIRECCIONES && !ventanas.containsKey(direccionIp)) {
            return false;
        }

        Ventana ventana = ventanas.compute(direccionIp, (ip, actual) -> {
            if (actual == null || actual.inicio.plusSeconds(DURACION_VENTANA_SEGUNDOS)
                    .isBefore(ahora)) {
                return new Ventana(ahora, 1);
            }
            return new Ventana(actual.inicio, actual.solicitudes + 1);
        });
        return ventana.solicitudes <= MAXIMO_POR_VENTANA;
    }

    private record Ventana(Instant inicio, int solicitudes) {
    }
}
