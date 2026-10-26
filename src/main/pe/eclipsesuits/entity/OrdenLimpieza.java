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
 * Orden de turnaround/limpieza de una habitación (RF-13 a RF-15).
 * Ciclo de vida de estadoLimpieza: PENDIENTE -> EN_LIMPIEZA -> LIMPIA.
 */
@Entity
@Table(name = "orden_limpieza")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenLimpieza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habitacion", nullable = false)
    @ToString.Exclude
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @Column(name = "estado_limpieza", nullable = false, length = 30)
    private String estadoLimpieza;

    @Column(name = "f_h_inicio_toque1", columnDefinition = "timestamptz")
    private OffsetDateTime fHInicioToque1;

    @Column(name = "f_h_fin_toque2", columnDefinition = "timestamptz")
    private OffsetDateTime fHFinToque2;

    @Column(name = "duracion_turnaround_min")
    private Short duracionTurnaroundMin;

    @Column(name = "observaciones_camarera", columnDefinition = "TEXT")
    private String observacionesCamarera;
}
