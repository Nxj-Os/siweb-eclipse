package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comanda global de room service generada desde la habitación.
 * {@code f_h_ultima_transicion} marca desde cuándo rige el estado
 * persistido y sirve de referencia para el avance automático por tiempo.
 */
@Entity
@Table(name = "pedido_room_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoRoomService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habitacion", nullable = false)
    @ToString.Exclude
    private Habitacion habitacion;

    @Column(name = "codigo_pedido", nullable = false, unique = true, length = 10)
    private String codigoPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 30)
    private EstadoPedido estadoPedido;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "f_h_pedido", nullable = false)
    private OffsetDateTime fHPedido;

    @Column(name = "f_h_ultima_transicion", nullable = false)
    private OffsetDateTime fHUltimaTransicion;

    @OneToMany(mappedBy = "pedido")
    @ToString.Exclude
    @Builder.Default
    private List<DetallePedidoRS> detalles = new ArrayList<>();
}
