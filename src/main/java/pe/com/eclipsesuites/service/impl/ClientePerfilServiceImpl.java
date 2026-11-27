package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dto.response.BoletaReservaDTO;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.dto.response.ReservaResumenDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IClientePerfilService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientePerfilServiceImpl implements IClientePerfilService {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BigDecimal IGV = new BigDecimal("0.18");
    private static final Set<EstadoReserva> ESTADOS_ACTIVOS = Set.of(
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, EstadoReserva.CHECKED_IN);

    private final IClienteAuthService clienteAuthService;
    private final ReservaEstadiaRepository reservaEstadiaRepository;

    @Override
    @Transactional(readOnly = true)
    public PerfilClienteResponseDTO obtenerPerfil(String correoElectronico) {
        Cliente cliente = clienteAuthService.buscarPorCorreo(correoElectronico);
        List<ReservaEstadia> reservas = reservasDe(cliente);

        ReservaResumenDTO activa = reservas.stream()
                .filter(this::esActiva)
                .findFirst()
                .map(this::aResumen)
                .orElse(null);

        return new PerfilClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                (cliente.getNombre() + " " + cliente.getApellido()).trim(),
                cliente.getCorreoElectronico(),
                cliente.getTipoDocumento(),
                cliente.getNumDocumento(),
                cliente.getPaisEmisionDoc(),
                cliente.getTelefono(),
                cliente.getEdad(),
                "CLIENTE",
                formatear(cliente.getFechaAceptacionPrivacidad()),
                cliente.estaActivo(),
                activa,
                reservas.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResumenDTO> listarReservas(String correoElectronico) {
        Cliente cliente = clienteAuthService.buscarPorCorreo(correoElectronico);
        return reservasDe(cliente).stream().map(this::aResumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaReservaDTO obtenerBoleta(String correoElectronico, Long idReserva) {
        ReservaEstadia reserva = reservaEstadiaRepository
                .findByIdAndClienteCorreoElectronicoIgnoreCase(idReserva, correoElectronico)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        return aBoleta(reserva);
    }

    private List<ReservaEstadia> reservasDe(Cliente cliente) {
        return reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(
                        cliente.getCorreoElectronico());
    }

    private boolean esActiva(ReservaEstadia reserva) {
        if (reserva.getEstadoReserva() == EstadoReserva.CONFIRMADA
                || reserva.getEstadoReserva() == EstadoReserva.CHECKED_IN) {
            return true;
        }
        return reserva.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO
                && reserva.getReservaTemporalHasta() != null
                && reserva.getReservaTemporalHasta().isAfter(java.time.OffsetDateTime.now());
    }

    private ReservaResumenDTO aResumen(ReservaEstadia reserva) {
        boolean activa = esActiva(reserva);
        return new ReservaResumenDTO(
                reserva.getId(),
                "Reserva #" + reserva.getId(),
                nombreSuite(reserva),
                reserva.getTipoHabitacion() == null ? null
                        : reserva.getTipoHabitacion().getNombreTematico(),
                habitacion(reserva),
                reserva.getEstadoReserva() == null ? null : reserva.getEstadoReserva().name(),
                reserva.getEstadoReserva() == null ? "Sin estado"
                        : reserva.getEstadoReserva().getEtiqueta(),
                formatear(reserva.getFechaInicio()),
                formatear(reserva.getFechaFin()),
                reserva.getModalidadTurno() == null ? null
                        : reserva.getModalidadTurno().getNombreTurno(),
                reserva.getCantidadPersonas(),
                reserva.getMetodoPago() == null ? null : reserva.getMetodoPago().getEtiqueta(),
                reserva.getMontoTotal(),
                formatear(reserva.getFechaCreacion()),
                imagen(reserva),
                activa);
    }

    private BoletaReservaDTO aBoleta(ReservaEstadia reserva) {
        BigDecimal total = reserva.getMontoTotal() == null ? BigDecimal.ZERO : reserva.getMontoTotal();
        BigDecimal subtotal = total.divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);
        Cliente cliente = reserva.getCliente();

        return new BoletaReservaDTO(
                reserva.getId(),
                String.format("001-%07d", reserva.getId()),
                "ECLIPSE SUITES HOTEL",
                cliente == null ? "" : (cliente.getNombre() + " " + cliente.getApellido()).trim(),
                cliente == null ? "" : cliente.getTipoDocumento(),
                cliente == null ? "" : cliente.getNumDocumento(),
                cliente == null ? "" : cliente.getCorreoElectronico(),
                nombreSuite(reserva),
                habitacion(reserva),
                reserva.getModalidadTurno() == null ? "-" : reserva.getModalidadTurno().getNombreTurno(),
                formatear(reserva.getFechaInicio()),
                formatear(reserva.getFechaFin()),
                reserva.getEstadoReserva() == null ? "-" : reserva.getEstadoReserva().getEtiqueta(),
                reserva.getMetodoPago() == null ? "-" : reserva.getMetodoPago().getEtiqueta(),
                reserva.getCantidadPersonas(),
                subtotal,
                igv,
                total,
                formatear(reserva.getFechaCreacion()));
    }

    private static String nombreSuite(ReservaEstadia reserva) {
        if (reserva.getTipoHabitacion() != null) {
            return reserva.getTipoHabitacion().getNombreTematico();
        }
        return "Suite";
    }

    private static String habitacion(ReservaEstadia reserva) {
        return reserva.getHabitacion() == null ? "Por asignar" : reserva.getHabitacion().getNumHabitacion();
    }

    private static String imagen(ReservaEstadia reserva) {
        if (reserva.getTipoHabitacion() == null
                || reserva.getTipoHabitacion().getGaleriaFotos() == null) {
            return null;
        }
        String galeria = reserva.getTipoHabitacion().getGaleriaFotos().trim();
        if (galeria.isEmpty()) {
            return null;
        }
        int corte = galeria.indexOf(',');
        return (corte > 0 ? galeria.substring(0, corte) : galeria).trim();
    }

    private static String formatear(LocalDateTime fecha) {
        return fecha == null ? "-" : fecha.format(FORMATO);
    }

    private static String formatear(Instant fecha) {
        if (fecha == null) {
            return "-";
        }
        return formatear(fecha.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
