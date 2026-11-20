package pe.com.eclipsesuites.dto.response;

import java.util.List;

public record ChatHistoricoResponseDTO(
        List<ChatMensajeHistoricoDTO> mensajes,
        String estadoCola,
        int minutosRestantes,
        String modo) {
}
