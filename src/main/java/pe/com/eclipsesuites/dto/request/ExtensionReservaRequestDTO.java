package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtensionReservaRequestDTO {

    @NotNull(message = "Selecciona la duración adicional.")
    private Integer modalidadId;
}
