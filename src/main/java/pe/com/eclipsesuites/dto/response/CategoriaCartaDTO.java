package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Categoría de la carta tal como la ve el huésped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaCartaDTO {

    private Integer idCategoria;
    private String nombre;
}
