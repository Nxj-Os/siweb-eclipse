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

@Entity
@Table(name = "tipo_habitacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_habitacion")
    private Integer id;

    @Column(name = "cod_tipo", nullable = false, unique = true, length = 20)
    private String codTipo;

    @Column(name = "nombre_tematico", nullable = false, length = 100)
    private String nombreTematico;

    @Column(name = "descripcion_tematica", nullable = false, columnDefinition = "TEXT")
    private String descripcionTematica;

    @Column(name = "galeria_fotos", nullable = false, columnDefinition = "TEXT")
    private String galeriaFotos;

    @Column(name = "capacidad_maxima", nullable = false)
    private Short capacidadMaxima;
}
