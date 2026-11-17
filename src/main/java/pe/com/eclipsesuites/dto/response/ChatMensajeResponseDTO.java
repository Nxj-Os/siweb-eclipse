package pe.com.eclipsesuites.dto.response;

import java.util.List;

public record ChatMensajeResponseDTO(
        String respuesta,
        String intencion,
        boolean derivado,
        String estadoCola,
        int minutosRestantes,
        List<String> sugerencias,
        String modo,
        int mensajesUsados,
        int mensajesMaximos) {
}
