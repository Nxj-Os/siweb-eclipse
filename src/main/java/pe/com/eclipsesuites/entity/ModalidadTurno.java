package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "modalidad_turno")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModalidadTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modalidad")
    private Integer id;

    @Column(name = "nombre_turno", nullable = false, unique = true, length = 50)
    private String nombreTurno;

    @Column(name = "duracion_horas", nullable = false, precision = 4, scale = 2)
    private BigDecimal duracionHoras;

    @Column(name = "hora_inicio_fija", nullable = false)
    private LocalTime horaInicioFija;

    @Column(name = "hora_fin_fija", nullable = false)
    private LocalTime horaFinFija;

    @Column(name = "permite_amanecida", nullable = false)
    private Boolean permiteAmanecida;
}
