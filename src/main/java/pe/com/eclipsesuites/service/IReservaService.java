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

    ReservaEstadia registrarMetodoPagoReserva(String correoCliente, Long reservaId,
                                              MetodoPagoReserva metodoPago);

    List<ReservaEstadia> listarReservasCliente(String correoCliente);

    Optional<ReservaEstadia> buscarReservaCliente(Long id, String correoCliente);

    ReservaEstadia cancelarReservaCliente(Long id, String correoCliente);

    List<ReservaEstadia> listarReservasPendientes();

    ReservaEstadia actualizarEstadoPendiente(Long id, EstadoReserva nuevoEstado);

    ReservaEstadia registrarCheckIn(String tokenQr);

    List<ReservaEstadia> listarReservasPendientesCheckIn();

    List<ReservaEstadia> listarReservasEnCurso();

    ExtensionEstadia crearExtension(String correoCliente, Long reservaId, Integer modalidadId);

    Optional<ExtensionEstadia> buscarExtensionCliente(Long extensionId, String correoCliente);

    ExtensionEstadia simularPagoExtension(String correoCliente, Long extensionId,
                                          MetodoPagoReserva metodoPago);
}