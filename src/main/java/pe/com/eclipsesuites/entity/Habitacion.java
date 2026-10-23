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

@Entity
@Table(name = "habitacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habitacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_habitacion", nullable = false)
    @ToString.Exclude
    private TipoHabitacion tipoHabitacion;

    @Column(name = "num_habitacion", nullable = false, unique = true, length = 10)
    private String numHabitacion;

    @Column(name = "piso", nullable = false)
    private Short piso;

    @Column(name = "estado_rack", nullable = false, length = 30)
    private String estadoRack;

    @Column(name = "token_qr_dinamico", nullable = false, unique = true, length = 255)
    private String tokenQrDinamico;

    @Column(name = "fecha_expiracion_token", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime fechaExpiracionToken;

    @Column(name = "bloqueo_temporal_hasta", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime bloqueoTemporalHasta;
}
