package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Carta de room service disponible para la suite: categorías,
 * productos con precio/stock, el número de suite del huésped (cuando
 * llega en el token) y las suites ocupadas para el selector de entrega.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaResponseDTO {

    private String numHabitacion;
    private List<String> suitesDisponibles;
    private List<CategoriaCartaDTO> categorias;
    private List<ProductoCartaDTO> productos;
}
