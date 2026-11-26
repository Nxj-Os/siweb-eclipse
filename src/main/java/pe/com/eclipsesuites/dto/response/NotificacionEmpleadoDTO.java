package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aviso del tablero del empleado: mezcla las últimas reservas web con las
 * últimas comandas de room service para que recepción y room service vean
 * la actividad del hotel en un solo lado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEmpleadoDTO {

    /** Clave estable para evitar repetir el mismo aviso en cada refresco. */
    private String id;
    /** RESERVA, CHECK_IN o COMANDA. */
    private String tipo;
    private String titulo;
    private String detalle;
    /** Estado de la reserva o de la comanda. */
    private String estado;
    /** Fecha del evento (se serializa como arreglo de Jackson). */
    private java.time.LocalDateTime fecha;
}
