package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Habitación física de una suite tal como se muestra en el detalle del catálogo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitacionCatalogoDTO {

    private Integer id;

    private String numero;

    private Short piso;

    private String estadoRack;

    private boolean disponible;
}
