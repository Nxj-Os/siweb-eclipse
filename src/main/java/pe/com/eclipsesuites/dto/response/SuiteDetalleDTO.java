package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficha completa de una suite: datos de tarjeta, tarifas y habitaciones.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuiteDetalleDTO {

    private Integer id;

    private String codTipo;

    private String nombre;

    private String descripcion;

    private String imagen;

    private Short capacidadMaxima;

    private int habitacionesTotales;

    private int habitacionesDisponibles;

    private boolean disponible;

    private BigDecimal precioDesde;

    private String modalidadDesde;

    @Builder.Default
    private List<String> amenidades = new ArrayList<>();

    @Builder.Default
    private List<TarifaCatalogoDTO> tarifas = new ArrayList<>();

    @Builder.Default
    private List<HabitacionCatalogoDTO> habitaciones = new ArrayList<>();

    @JsonIgnore
    public String getPrecioDesdeTexto() {
        return precioDesde == null
                ? null
                : "S/ " + precioDesde.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
