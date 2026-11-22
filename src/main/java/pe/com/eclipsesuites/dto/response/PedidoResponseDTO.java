package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Comanda del huésped: código, estado efectivo (con avance automático),
 * monto total congelado y contenido del pedido (Figma C-09).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponseDTO {

    private String codigo;
    private String estado;
    private String estadoEtiqueta;
    private BigDecimal montoTotal;
    private OffsetDateTime fHPedido;
    private String numHabitacion;
    private List<PedidoItemResponseDTO> items;
}
