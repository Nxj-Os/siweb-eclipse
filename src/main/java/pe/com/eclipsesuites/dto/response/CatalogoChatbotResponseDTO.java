package pe.com.eclipsesuites.dto.response;

import java.util.List;

public record CatalogoChatbotResponseDTO(
        List<HabitacionCardDTO> habitaciones,
        int total,
        int disponibles,
        String fechaConsulta) {
}
