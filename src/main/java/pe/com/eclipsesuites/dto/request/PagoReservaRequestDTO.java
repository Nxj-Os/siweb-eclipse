package pe.com.eclipsesuites.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;

@Data
public class PagoReservaRequestDTO {

    @NotNull(message = "Selecciona un método de pago.")
    private MetodoPagoReserva metodoPago;
}
