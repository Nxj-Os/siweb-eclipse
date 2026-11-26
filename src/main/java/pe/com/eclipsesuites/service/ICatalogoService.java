package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.response.DisponibilidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.ModalidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteDetalleDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio del catálogo público de suites.
 */
public interface ICatalogoService {

    /**
     * Lista las suites con su disponibilidad y precio "desde".
     *
     * @param fecha       fecha de ingreso deseada (opcional)
     * @param hora        hora de ingreso deseada (opcional)
     * @param modalidadId modalidad de turno (opcional; por defecto la de menor duración)
     */
    List<SuiteCatalogoDTO> listarSuites(LocalDate fecha, LocalTime hora, Integer modalidadId);

    Optional<SuiteDetalleDTO> obtenerDetalle(String codTipo, LocalDate fecha, LocalTime hora,
                                             Integer modalidadId);

    /**
     * Consulta la disponibilidad real de una suite considerando las reservas
     * activas que se cruzan con el rango solicitado.
     */
    DisponibilidadCatalogoDTO consultarDisponibilidad(Integer tipoHabitacionId, LocalDate fecha,
                                                      LocalTime hora, Integer modalidadId);

    List<ModalidadCatalogoDTO> listarModalidades();
}
