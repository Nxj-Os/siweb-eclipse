package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BloqueoHabitacionRequestDTO {

    @NotBlank(message = "Indica el motivo del bloqueo.")
    @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
    private String motivo;
}
