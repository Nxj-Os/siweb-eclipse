package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Línea del pedido tal como la ve el huésped en el resumen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoItemResponseDTO {

    private String nombre;
    private Integer cantidad;
    private BigDecimal subtotal;
}
