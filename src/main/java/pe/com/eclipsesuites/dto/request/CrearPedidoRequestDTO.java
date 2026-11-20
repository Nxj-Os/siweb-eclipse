package pe.com.eclipsesuites.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cuerpo para crear un pedido de room service desde la suite.
 * El token de habitación identifica el check-in validado del huésped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPedidoRequestDTO {

    @NotBlank(message = "El token de habitación es obligatorio")
    private String token;

    @NotEmpty(message = "El pedido debe incluir al menos un producto")
    private List<@Valid PedidoItemDTO> items;
}
