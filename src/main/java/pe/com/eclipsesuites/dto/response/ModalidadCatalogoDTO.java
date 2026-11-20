package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modalidad de turno publicada en el catálogo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModalidadCatalogoDTO {

    private Integer id;

    private String nombre;

    private BigDecimal duracionHoras;
}
