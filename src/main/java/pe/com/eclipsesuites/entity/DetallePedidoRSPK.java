package pe.com.eclipsesuites.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Clave primaria compuesta de detalle_pedido_rs: (id_pedido, id_producto).
 * Descompone la relación M:N entre la comanda y el producto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoRSPK implements Serializable {

    private Integer idPedido;
    private Integer idProducto;
}
