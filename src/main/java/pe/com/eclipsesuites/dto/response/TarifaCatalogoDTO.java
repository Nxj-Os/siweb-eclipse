package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tarifa de una suite para una modalidad de turno concreta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaCatalogoDTO {

    private Integer modalidadId;

    private String modalidad;

    private BigDecimal duracionHoras;

    private BigDecimal precio;

    private BigDecimal costoHoraAdicional;

    private boolean vigente;

    @JsonIgnore
    public String getPrecioTexto() {
        return formatear(precio);
    }

    @JsonIgnore
    public String getCostoHoraAdicionalTexto() {
        return formatear(costoHoraAdicional);
    }

    private String formatear(BigDecimal valor) {
        return valor == null
                ? null
                : "S/ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
