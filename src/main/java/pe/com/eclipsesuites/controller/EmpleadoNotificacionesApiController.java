package pe.com.eclipsesuites.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dto.response.NotificacionEmpleadoDTO;
import pe.com.eclipsesuites.dto.response.PedidoItemResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.service.IRoomServiceService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tablero de novedades del portal de empleados: junta las últimas reservas
 * web con las últimas comandas de room service para que recepción vea la
 * demanda y room service vea qué suites han hecho check-in.
 * (La ruta {@code /api/empleado/notificaciones} ya la ocupa el módulo de
 * agenda: {@link AgendaEmpleadoController}.)
 */
@RestController
@RequestMapping("/api/empleado/novedades")
@RequiredArgsConstructor
public class EmpleadoNotificacionesApiController {

    private static final int LIMITE_RESERVAS = 10;
    private static final int LIMITE_AVISOS = 12;

    private final ReservaEstadiaRepository reservaEstadiaRepository;
    private final IRoomServiceService roomServiceService;

    public record NotificacionesResponseDTO(ResumenNotificacionesDTO resumen,
                                            List<NotificacionEmpleadoDTO> notificaciones) {}

    public record ResumenNotificacionesDTO(long reservasNuevas,
                                           long pendientesCheckIn,
                                           long reservasEnCurso,
                                           long comandasActivas) {}

    @GetMapping
    public ResponseEntity<NotificacionesResponseDTO> notificaciones() {
        List<ReservaEstadia> reservas =
                reservaEstadiaRepository.buscarRecientesConCliente(PageRequest.of(0, LIMITE_RESERVAS));
        List<PedidoResponseDTO> pedidos = roomServiceService.listarPedidos();

        List<NotificacionEmpleadoDTO> avisos = new ArrayList<>();
        reservas.forEach(reserva -> avisos.add(avisoDeReserva(reserva)));
        pedidos.forEach(pedido -> avisos.add(avisoDeComanda(pedido)));

        List<NotificacionEmpleadoDTO> ordenados = avisos.stream()
                .sorted(Comparator.comparing(NotificacionEmpleadoDTO::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMITE_AVISOS)
                .toList();

        long comandasActivas = pedidos.stream()
                .map(PedidoResponseDTO::getEstado)
                .filter(estado -> "RECIBIDO".equals(estado) || "EN_PREPARACION".equals(estado))
                .count();

        ResumenNotificacionesDTO resumen = new ResumenNotificacionesDTO(
                reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.PENDIENTE_PAGO),
                reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.CONFIRMADA),
                reservaEstadiaRepository.countByEstadoReserva(EstadoReserva.CHECKED_IN),
                comandasActivas);

        return ResponseEntity.ok(new NotificacionesResponseDTO(resumen, ordenados));
    }

    private NotificacionEmpleadoDTO avisoDeReserva(ReservaEstadia reserva) {
        String numSuite = reserva.getHabitacion() == null ? null
                : reserva.getHabitacion().getNumHabitacion();
        EstadoReserva estado = reserva.getEstadoReserva();

        String titulo = switch (estado) {
            case PENDIENTE_PAGO -> "Nueva reserva #" + reserva.getId();
            case CONFIRMADA -> "Reserva confirmada #" + reserva.getId();
            case CHECKED_IN -> "Check-in en suite " + (numSuite == null ? "—" : numSuite);
            case CANCELADA -> "Reserva cancelada #" + reserva.getId();
            case EXPIRADA -> "Reserva expirada #" + reserva.getId();
        };
        String tipo = estado == EstadoReserva.CHECKED_IN ? "CHECK_IN" : "RESERVA";
        String suite = numSuite == null ? "sin suite" : "suite " + numSuite;
        String detalle = String.format(Locale.ROOT, "%s · %s · %d personas · S/ %s",
                reserva.getTipoHabitacion() == null ? "Suite" : reserva.getTipoHabitacion().getNombreTematico(),
                suite,
                reserva.getCantidadPersonas() == null ? 0 : reserva.getCantidadPersonas(),
                reserva.getMontoTotal() == null ? "0.00"
                        : String.format(Locale.ROOT, "%.2f", reserva.getMontoTotal()));
        LocalDateTime fecha = reserva.getFechaCheckIn() != null ? reserva.getFechaCheckIn()
                : (reserva.getFechaCreacion() != null ? reserva.getFechaCreacion()
                : reserva.getFechaInicio());

        return NotificacionEmpleadoDTO.builder()
                .id("reserva-" + reserva.getId())
                .tipo(tipo)
                .titulo(titulo)
                .detalle(detalle)
                .estado(estado == null ? null : estado.name())
                .fecha(fecha)
                .build();
    }

    private NotificacionEmpleadoDTO avisoDeComanda(PedidoResponseDTO pedido) {
        String suite = pedido.getNumHabitacion() == null ? "—" : pedido.getNumHabitacion();
        List<PedidoItemResponseDTO> items = pedido.getItems();
        String contenido;
        if (items == null || items.isEmpty()) {
            contenido = "sin ítems";
        } else if (items.size() > 2) {
            contenido = items.size() + " ítems";
        } else {
            contenido = items.stream()
                    .map(item -> item.getCantidad() + "× " + item.getNombre())
                    .toList().toString();
            contenido = contenido.substring(1, contenido.length() - 1);
        }
        java.math.BigDecimal monto = pedido.getMontoTotal();

        return NotificacionEmpleadoDTO.builder()
                .id("comanda-" + pedido.getCodigo())
                .tipo("COMANDA")
                .titulo("Comanda " + pedido.getCodigo() + " · suite " + suite)
                .detalle(monto == null ? contenido
                        : String.format(Locale.ROOT, "%s · S/ %.2f", contenido, monto))
                .estado(pedido.getEstado())
                .fecha(pedido.getFHPedido() == null ? null
                        : pedido.getFHPedido().toLocalDateTime())
                .build();
    }
}
