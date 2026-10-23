package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteLoginRequestDTO {

    @NotBlank
    @Email
    @Size(max = 100)
    private String correoElectronico;

    @NotBlank
    @Size(max = 72)
    private String password;
}