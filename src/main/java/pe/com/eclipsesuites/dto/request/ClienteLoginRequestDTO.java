package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteLoginRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;
}