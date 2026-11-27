package pe.com.eclipsesuites.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cuerpo para crear un pedido de room service desde la suite.
 * El token de habitación identifica el check-in validado del huésped;
 * sin check-in, el huésped elige la suite de entrega con
 * {@code numHabitacion}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPedidoRequestDTO {

    /** Token de check-in (opcional hasta que se integre el QR). */
    private String token;

    /** Número de suite de entrega (opcional; se usa si no hay token). */
    private String numHabitacion;

    @NotEmpty(message = "El pedido debe incluir al menos un producto")
    private List<@Valid PedidoItemDTO> items;
}
