package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tarifa_habitacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaHabitacion {

    @EmbeddedId
    private TarifaHabitacionPK id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idTipoHabitacion")
    @JoinColumn(name = "id_tipo_habitacion", nullable = false)
    @ToString.Exclude
    private TipoHabitacion tipoHabitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idModalidad")
    @JoinColumn(name = "id_modalidad", nullable = false)
    @ToString.Exclude
    private ModalidadTurno modalidadTurno;

    @Column(name = "precio_estadia_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioEstadiaBase;

    @Column(name = "costo_hora_adicional", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoHoraAdicional;

    @Column(name = "es_tarifa_fin_semana", nullable = false)
    private Boolean esTarifaFinSemana;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta", nullable = false)
    private LocalDate vigenciaHasta;
}
