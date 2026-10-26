package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;

/**
 * Reporte de avería técnica de una habitación (RF-16).
 * gravedad esperada: BAJA, MEDIA, ALTA, CRITICA.
 */
@Entity
@Table(name = "reporte_averia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteAveria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_averia")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habitacion", nullable = false)
    @ToString.Exclude
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @Column(name = "titulo_incidencia", nullable = false, length = 150)
    private String tituloIncidencia;

    @Column(name = "descripcion_dano", columnDefinition = "TEXT")
    private String descripcionDano;

    @Column(name = "gravedad", nullable = false, length = 20)
    private String gravedad;

    @Column(name = "bloquea_habitacion", nullable = false)
    private Boolean bloqueaHabitacion;

    @Column(name = "f_h_reporte", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime fHReporte;
}
