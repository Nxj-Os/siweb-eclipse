package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Línea de la comanda: cantidad solicitada, precio unitario congelado
 * al momento del pedido y subtotal de la línea. Su clave primaria es
 * compuesta (id_pedido, id_producto); las asociaciones son de solo
 * lectura porque las columnas las gobiernan los campos {@code @Id}.
 */
@Entity
@Table(name = "detalle_pedido_rs")
@IdClass(DetallePedidoRSPK.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePedidoRS {

    @Id
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", insertable = false, updatable = false)
    @ToString.Exclude
    private PedidoRoomService pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", insertable = false, updatable = false)
    @ToString.Exclude
    private Producto producto;

    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;

    @Column(name = "precio_unitario_congelado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitarioCongelado;

    @Column(name = "subtotal_linea", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalLinea;
}
