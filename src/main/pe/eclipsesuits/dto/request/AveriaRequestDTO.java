package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AveriaRequestDTO {

    @NotNull(message = "La habitación es obligatoria")
    private Integer idHabitacion;

    @NotNull(message = "El empleado que reporta es obligatorio")
    private Integer idEmpleado;

    @NotBlank(message = "El título de la incidencia es obligatorio")
    @Size(max = 150)
    private String tituloIncidencia;

    @NotBlank(message = "La descripción del daño es obligatoria")
    private String descripcionDano;

    @NotBlank(message = "La gravedad es obligatoria")
    @Size(max = 20)
    private String gravedad;
}
