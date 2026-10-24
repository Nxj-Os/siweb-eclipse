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

    @NotBlank
    @Size(max = 10)
    private String tipoDocumento;

    @NotBlank
    @Size(max = 20)
    private String numDocumento;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}", message = "Usa el código de país de 3 letras")
    private String paisEmisionDoc;

    @NotBlank
    @Email
    @Size(max = 100)
    private String correoElectronico;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    private String apellido;

    @NotBlank
    @Size(max = 20)
    private String telefono;

    @NotNull
    @Min(1)
    @Max(120)
    private Integer edad;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;
}