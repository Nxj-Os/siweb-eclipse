package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;

@Data
public class PagoReservaRequestDTO {

    @NotNull(message = "Selecciona un método de pago.")
    private MetodoPagoReserva metodoPago;

    @Size(max = 100, message = "La referencia de pago no puede exceder 100 caracteres.")
    private String referenciaPago;

    private String numeroYape;

    private String codigoAprobacion;

    public String referenciaYapeFormateada() {
        String numero = numeroYape == null ? "" : numeroYape.replaceAll("\\D", "");
        String codigo = codigoAprobacion == null ? "" : codigoAprobacion.replaceAll("\\D", "");
        if (numero.isBlank() || codigo.isBlank()) {
            return null;
        }
        return "Celular: " + numero + " | Código de aprobación: " + codigo;
    }
}
