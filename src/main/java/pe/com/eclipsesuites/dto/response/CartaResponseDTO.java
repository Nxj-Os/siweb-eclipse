package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Carta de room service disponible para la habitación: categorías,
 * productos con precio/stock y el número de suite del huésped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaResponseDTO {

    private String numHabitacion;
    private List<CategoriaCartaDTO> categorias;
    private List<ProductoCartaDTO> productos;
}
