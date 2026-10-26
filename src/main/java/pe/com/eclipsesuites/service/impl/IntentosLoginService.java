package pe.com.eclipsesuites.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class IntentosLoginService {

    private static final int MAXIMO_INTENTOS = 5;
    private static final long DURACION_BLOQUEO_SEGUNDOS = 3 * 60;
    private static final int MAXIMO_CLAVES = 100_000;

    private final ConcurrentMap<String, EstadoIntentos> intentos = new ConcurrentHashMap<>();

    public void verificarPermitido(String ip, String identidad) {
        String clave = crearClave(ip, identidad);
        EstadoIntentos estado = intentos.get(clave);
        if (estado == null || estado.bloqueadoHasta == null) {
            return;
        }

        long segundosRestantes = Math.max(0,
                estado.bloqueadoHasta.getEpochSecond() - Instant.now().getEpochSecond());
        if (segundosRestantes <= 0) {
            intentos.remove(clave, estado);
            return;
        }
        throw new LoginTemporalmenteBloqueadoException(segundosRestantes);
    }

    public void registrarFallo(String ip, String identidad) {
        limpiarBloqueosExpirados();
        String clave = crearClave(ip, identidad);
        if (intentos.size() >= MAXIMO_CLAVES && !intentos.containsKey(clave)) {
            throw new LoginTemporalmenteBloqueadoException(DURACION_BLOQUEO_SEGUNDOS);
        }

        Instant ahora = Instant.now();
        EstadoIntentos estado = intentos.compute(clave, (key, anterior) -> {
            int cantidad = anterior == null || anterior.inicio.plusSeconds(DURACION_BLOQUEO_SEGUNDOS)
                    .isBefore(ahora) ? 1 : anterior.cantidad + 1;
            Instant bloqueadoHasta = cantidad >= MAXIMO_INTENTOS
                    ? ahora.plusSeconds(DURACION_BLOQUEO_SEGUNDOS)
                    : null;
            return new EstadoIntentos(cantidad, anterior == null
                    || anterior.inicio.plusSeconds(DURACION_BLOQUEO_SEGUNDOS).isBefore(ahora)
                    ? ahora : anterior.inicio, bloqueadoHasta);
        });
        log.warn("Intento de autenticación fallido (referencia {}); acumulados: {}",
                clave.substring(0, 16), estado.cantidad);
    }

    public void registrarExito(String ip, String identidad) {
        intentos.remove(crearClave(ip, identidad));
    }

    private void limpiarBloqueosExpirados() {
        Instant ahora = Instant.now();
        intentos.entrySet().removeIf(entry ->
                entry.getValue().inicio.plusSeconds(DURACION_BLOQUEO_SEGUNDOS).isBefore(ahora));
    }

    private String crearClave(String ip, String identidad) {
        String material = normalizar(ip) + "\u0000" + normalizar(identidad);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No está disponible SHA-256 para proteger el registro de accesos", ex);
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record EstadoIntentos(int cantidad, Instant inicio, Instant bloqueadoHasta) {
    }
}
