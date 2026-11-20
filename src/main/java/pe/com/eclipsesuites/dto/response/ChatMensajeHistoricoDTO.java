package pe.com.eclipsesuites.dto.response;

import java.time.OffsetDateTime;

public record ChatMensajeHistoricoDTO(
        String rol,
        String contenido,
        OffsetDateTime fhMensaje,
        String intencion) {
}
