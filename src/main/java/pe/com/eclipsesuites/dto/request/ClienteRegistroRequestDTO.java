package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteRegistroRequestDTO {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(DNI|CE|PASAPORTE)$", message = "El tipo de documento debe ser DNI, CE o PASAPORTE")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 20, message = "El número de documento debe tener entre 8 y 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El número de documento solo debe contener letras y números")
    private String numDocumento;

    @NotBlank(message = "El país emisor es obligatorio")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Usa el código de país de 3 letras")
    private String paisEmisionDoc;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El nombre solo debe contener letras")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El apellido solo debe contener letras")
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 25, message = "El teléfono debe tener entre 7 y 25 caracteres")
    @Pattern(regexp = "^[0-9+\\s()-]+$", message = "El teléfono solo debe contener números, +, espacios, paréntesis o guiones")
    private String telefono;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debes ser mayor de 18 años")
    @Max(value = 120, message = "La edad debe ser válida")
    private Integer edad;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;
}