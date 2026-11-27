package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.request.ReservaRequestDTO;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

public interface IReservaService {

    List<TipoHabitacion> listarTiposHabitacion();

    List<ModalidadTurno> listarModalidades();

    List<TarifaHabitacion> listarTarifas();

    List<Habitacion> listarHabitacionesPorTipo(Integer tipoHabitacionId);

    Set<Integer> listarHabitacionesDisponibles(Integer tipoHabitacionId,
                                               LocalDateTime inicio,
                                               LocalDateTime fin);

    ReservaEstadia crearReserva(String correoCliente, ReservaRequestDTO request);

    ReservaEstadia simularPagoReserva(String correoCliente, Long reservaId,
                                      MetodoPagoReserva metodoPago);

    ReservaEstadia simularPagoReserva(String correoCliente, Long reservaId,
                                      MetodoPagoReserva metodoPago, String referenciaPago);

    ReservaEstadia registrarMetodoPagoReserva(String correoCliente, Long reservaId,
                                              MetodoPagoReserva metodoPago);

    ReservaEstadia registrarMetodoPagoReserva(String correoCliente, Long reservaId,
                                              MetodoPagoReserva metodoPago, String referenciaPago);

    List<ReservaEstadia> listarReservasCliente(String correoCliente);

    Optional<ReservaEstadia> buscarReservaCliente(Long id, String correoCliente);

    ReservaEstadia cancelarReservaCliente(Long id, String correoCliente);

    List<ReservaEstadia> listarReservasPendientes();

    ReservaEstadia actualizarEstadoPendiente(Long id, EstadoReserva nuevoEstado);

    ReservaEstadia registrarCheckIn(String tokenQr);

    /**
     * Check-in hecho por el huésped desde su cuenta: valida propiedad y
     * vigencia y deja la suite {@code OCUPADA} en el rack.
     */
    ReservaEstadia registrarCheckInCliente(Long reservaId, String correoCliente);

    List<ReservaEstadia> listarReservasPendientesCheckIn();

    List<ReservaEstadia> listarReservasEnCurso();

    Habitacion bloquearHabitacion(Integer habitacionId, String motivo);

    Habitacion liberarHabitacion(Integer habitacionId);

    ExtensionEstadia extenderReservaRecepcion(Long reservaId, Integer horas);

    ExtensionEstadia crearExtension(String correoCliente, Long reservaId, Integer modalidadId);

    Optional<ExtensionEstadia> buscarExtensionCliente(Long extensionId, String correoCliente);

    ExtensionEstadia simularPagoExtension(String correoCliente, Long extensionId,
                                          MetodoPagoReserva metodoPago);

    ExtensionEstadia simularPagoExtension(String correoCliente, Long extensionId,
                                          MetodoPagoReserva metodoPago, String referenciaPago);

    ExtensionEstadia registrarMetodoPagoExtension(String correoCliente, Long extensionId,
                                                  MetodoPagoReserva metodoPago);

    ExtensionEstadia registrarMetodoPagoExtension(String correoCliente, Long extensionId,
                                                  MetodoPagoReserva metodoPago, String referenciaPago);

    ExtensionEstadia cancelarExtensionCliente(String correoCliente, Long extensionId);

    List<ExtensionEstadia> listarExtensionesPendientesValidacion();

    ExtensionEstadia validarExtensionRecepcion(Long extensionId, boolean aprobar);
}
