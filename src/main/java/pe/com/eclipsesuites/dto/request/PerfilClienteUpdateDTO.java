package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Campos editables del perfil del cliente. El correo y el documento no se
 * modifican desde aquí porque tienen restricción de unicidad.
 */
@Data
public class PerfilClienteUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El nombre solo debe contener letras")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El apellido solo debe contener letras")
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 9, max = 16, message = "El teléfono debe tener formato internacional")
    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
            message = "Usa el formato internacional E.164, por ejemplo +51999123456")
    private String telefono;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debes ser mayor de 18 años")
    @Max(value = 120, message = "La edad debe ser válida")
    private Integer edad;
}
