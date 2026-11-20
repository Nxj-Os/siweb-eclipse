package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo para avanzar el estado de una comanda. Lo consume el sistema
 * de empleado autenticado (rol ROOMSERVICE o ADMIN); el avance normal
 * ocurre automáticamente con el tiempo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarEstadoPedidoRequestDTO {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
}
