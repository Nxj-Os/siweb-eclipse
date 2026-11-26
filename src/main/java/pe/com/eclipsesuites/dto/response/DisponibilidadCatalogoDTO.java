package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de consultar disponibilidad de una suite para una fecha, hora y
 * modalidad concretas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadCatalogoDTO {

    private Integer tipoHabitacionId;

    private String codTipo;

    private String nombre;

    private LocalDate fecha;

    private LocalTime horaInicio;

    private LocalTime horaFin;

    private Integer modalidadId;

    private String modalidad;

    private int habitacionesTotales;

    private int habitacionesDisponibles;

    private boolean disponible;

    @Builder.Default
    private List<Integer> idsHabitacion = new ArrayList<>();

    @Builder.Default
    private List<String> numerosHabitacion = new ArrayList<>();
}
