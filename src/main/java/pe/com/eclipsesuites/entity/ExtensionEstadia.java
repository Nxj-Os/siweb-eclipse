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
@Table(name = "extension_estadia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionEstadia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_extension")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    @ToString.Exclude
    private ReservaEstadia reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidad")
    @ToString.Exclude
    private ModalidadTurno modalidadTurno;

    @Column(name = "duracion_horas", precision = 4, scale = 2)
    private BigDecimal duracionHoras;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    @Builder.Default
    private MetodoPagoReserva metodoPago = MetodoPagoReserva.EFECTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoExtensionReserva estado = EstadoExtensionReserva.PENDIENTE_PAGO;

    @Column(name = "reserva_temporal_hasta", nullable = false)
    private OffsetDateTime reservaTemporalHasta;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false,
            columnDefinition = "varchar(20) default 'PENDIENTE'")
    @Builder.Default
    private EstadoPagoReserva estadoPago = EstadoPagoReserva.PENDIENTE;

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;
}
