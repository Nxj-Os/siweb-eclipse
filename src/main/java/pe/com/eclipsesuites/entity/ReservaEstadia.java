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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reserva_estadia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaEstadia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_habitacion", nullable = false)
    @ToString.Exclude
    private TipoHabitacion tipoHabitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habitacion")
    @ToString.Exclude
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidad", nullable = false)
    @ToString.Exclude
    private ModalidadTurno modalidadTurno;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "cantidad_personas", nullable = false)
    private Integer cantidadPersonas;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPagoReserva metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoReserva estadoReserva = EstadoReserva.PENDIENTE_PAGO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "reserva_temporal_hasta", columnDefinition = "timestamptz")
    private OffsetDateTime reservaTemporalHasta;

    @Column(name = "token_qr", unique = true, length = 64)
    private String tokenQr;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_check_in")
    private LocalDateTime fechaCheckIn;
}