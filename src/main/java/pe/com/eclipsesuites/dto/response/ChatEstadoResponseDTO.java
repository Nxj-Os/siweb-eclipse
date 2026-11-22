package pe.com.eclipsesuites.dto.response;

public record ChatEstadoResponseDTO(
        String estadoCola,
        int minutosRestantes,
        String modo,
        int mensajesUsados,
        int mensajesMaximos,
        boolean derivado) {
}
