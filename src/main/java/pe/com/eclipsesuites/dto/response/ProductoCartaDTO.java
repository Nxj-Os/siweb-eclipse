package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Producto de la carta con precio y stock visibles para el huésped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCartaDTO {

    private Integer idProducto;
    private Integer idCategoria;
    private String categoria;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stockDisponible;
    private String imagen;
}
