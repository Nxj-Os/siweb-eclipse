package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaHabitacionPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_tipo_habitacion")
    private Integer idTipoHabitacion;

    @Column(name = "id_modalidad")
    private Integer idModalidad;
}
