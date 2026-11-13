package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dao.ExtensionEstadiaRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.ModalidadTurnoRepository;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dao.TarifaHabitacionRepository;
import pe.com.eclipsesuites.dao.TipoHabitacionRepository;
import pe.com.eclipsesuites.dto.request.ReservaRequestDTO;
import pe.com.eclipsesuites.entity.EstadoExtensionReserva;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IReservaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements IReservaService {

    private static final List<EstadoReserva> ESTADOS_QUE_OCUPAN_HABITACION =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.CHECKED_IN);
    private static final int MINUTOS_PARA_PAGAR = 5;

    private final ClienteRepository clienteRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final ModalidadTurnoRepository modalidadTurnoRepository;
    private final HabitacionRepository habitacionRepository;
    private final TarifaHabitacionRepository tarifaHabitacionRepository;
    private final ReservaEstadiaRepository reservaEstadiaRepository;
    private final ExtensionEstadiaRepository extensionEstadiaRepository;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Override
    @Transactional(readOnly = true)
    public List<TipoHabitacion> listarTiposHabitacion() {
        return tipoHabitacionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModalidadTurno> listarModalidades() {
        return modalidadTurnoRepository.findAll().stream()
                .sorted(Comparator.comparing(ModalidadTurno::getDuracionHoras))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaHabitacion> listarTarifas() {
        return tarifaHabitacionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Habitacion> listarHabitacionesPorTipo(Integer tipoHabitacionId) {
        if (!tipoHabitacionRepository.existsById(tipoHabitacionId)) {
            throw new IllegalArgumentException("La suite seleccionada no existe.");
        }
        return habitacionRepository.findByTipoHabitacionId(tipoHabitacionId).stream()
                .sorted(Comparator.comparing(Habitacion::getNumHabitacion))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> listarHabitacionesDisponibles(Integer tipoHabitacionId,
                                                       LocalDateTime inicio,
                                                       LocalDateTime fin) {
        return listarHabitacionesPorTipo(tipoHabitacionId).stream()
                .filter(habitacion -> estaDisponible(habitacion, inicio, fin))
                .map(Habitacion::getId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public ReservaEstadia crearReserva(String correoCliente, ReservaRequestDTO request) {
        var cliente = clienteRepository.findByCorreoElectronicoIgnoreCase(correoCliente)
                .orElseThrow(() -> new IllegalStateException("No se encontró el cliente de la sesión."));
        TipoHabitacion tipo = tipoHabitacionRepository.findById(request.getTipoHabitacionId())
                .orElseThrow(() -> new IllegalArgumentException("La suite seleccionada no existe."));
        ModalidadTurno modalidad = modalidadTurnoRepository.findById(request.getModalidadId())
                .orElseThrow(() -> new IllegalArgumentException("La duración seleccionada no existe."));
        Habitacion habitacion = habitacionRepository.findByIdForUpdate(request.getHabitacionId())
                .orElseThrow(() -> new IllegalArgumentException("La habitación seleccionada no existe."));

        if (!tipo.getId().equals(habitacion.getTipoHabitacion().getId())) {
            throw new IllegalArgumentException("La habitación no pertenece a la suite seleccionada.");
        }
        validarCapacidad(tipo, request.getCantidadPersonas());
        LocalDateTime inicio = calcularInicio(request.getFechaReserva(), request.getHoraInicio());
        LocalDateTime fin = calcularFin(inicio, modalidad);
        validarFechaFutura(inicio);
        TarifaHabitacion tarifa = buscarTarifa(tipo.getId(), modalidad.getId(), inicio.toLocalDate());
        validarDisponibilidad(habitacion, inicio, fin);

        ReservaEstadia reserva = ReservaEstadia.builder()
                .cliente(cliente)
                .tipoHabitacion(tipo)
                .habitacion(habitacion)
                .modalidadTurno(modalidad)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .cantidadPersonas(request.getCantidadPersonas())
                .montoTotal(tarifa.getPrecioEstadiaBase())
                .metodoPago(MetodoPagoReserva.EFECTIVO)
                .estadoReserva(EstadoReserva.PENDIENTE_PAGO)
                .fechaCreacion(LocalDateTime.now())
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(MINUTOS_PARA_PAGAR))
                .build();
        return reservaEstadiaRepository.save(reserva);
    }

    @Override
    @Transactional
    public ReservaEstadia simularPagoReserva(String correoCliente, Long reservaId,
                                             MetodoPagoReserva metodoPago) {
        if (!demoMode) {
            throw new IllegalStateException("El pago simulado solo está disponible en modo demo.");
        }
        ReservaEstadia reserva = buscarReservaPropia(reservaId, correoCliente);
        validarReservaPendienteDePago(reserva);
        return confirmarPago(reserva, metodoPago);
    }

    @Override
    @Transactional
    public ReservaEstadia registrarMetodoPagoReserva(String correoCliente, Long reservaId,
                                                      MetodoPagoReserva metodoPago) {
        ReservaEstadia reserva = buscarReservaPropia(reservaId, correoCliente);
        validarReservaPendienteDePago(reserva);
        reserva.setMetodoPago(metodoPago);
        return reservaEstadiaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaEstadia> listarReservasCliente(String correoCliente) {
        return reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(correoCliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservaEstadia> buscarReservaCliente(Long id, String correoCliente) {
        return reservaEstadiaRepository.findByIdAndClienteCorreoElectronicoIgnoreCase(id, correoCliente);
    }

    @Override
    @Transactional
    public ReservaEstadia cancelarReservaCliente(Long id, String correoCliente) {
        ReservaEstadia reserva = buscarReservaPropia(id, correoCliente);
        validarReservaPendienteDePago(reserva);
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        return reservaEstadiaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaEstadia> listarReservasPendientes() {
        return reservaEstadiaRepository.findByEstadoReservaOrderByFechaCreacionAsc(
                EstadoReserva.PENDIENTE_PAGO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaEstadia> listarReservasPendientesCheckIn() {
        return reservaEstadiaRepository.findByEstadoReservaOrderByFechaCreacionAsc(
                EstadoReserva.CONFIRMADA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaEstadia> listarReservasEnCurso() {
        return reservaEstadiaRepository.findByEstadoReservaOrderByFechaCreacionAsc(
                EstadoReserva.CHECKED_IN);
    }

    @Override
    @Transactional
    public ReservaEstadia actualizarEstadoPendiente(Long id, EstadoReserva nuevoEstado) {
        if (nuevoEstado != EstadoReserva.CONFIRMADA && nuevoEstado != EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("El estado solicitado no es válido.");
        }
        ReservaEstadia reserva = reservaEstadiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró esa reserva."));
        validarReservaPendienteDePago(reserva);
        if (nuevoEstado == EstadoReserva.CANCELADA) {
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            return reservaEstadiaRepository.save(reserva);
        }
        if (reserva.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El horario de esta reserva ya comenzó.");
        }
        return confirmarPago(reserva, reserva.getMetodoPago());
    }

    @Override
    @Transactional
    public ReservaEstadia registrarCheckIn(String tokenQr) {
        ReservaEstadia reserva = reservaEstadiaRepository.findByTokenQr(tokenQr)
                .orElseThrow(() -> new IllegalArgumentException("El código QR no es válido."));
        if (reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException("El QR ya fue usado o la reserva no está confirmada.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(reserva.getFechaInicio()) || !ahora.isBefore(reserva.getFechaFin())) {
            throw new IllegalStateException("El horario de esta reserva no está vigente.");
        }
        reserva.setEstadoReserva(EstadoReserva.CHECKED_IN);
        reserva.setFechaCheckIn(ahora);
        return reservaEstadiaRepository.save(reserva);
    }

    @Override
    @Transactional
    public ExtensionEstadia crearExtension(String correoCliente, Long reservaId,
                                            Integer modalidadId) {
        if (!demoMode) {
            throw new IllegalStateException("Las ampliaciones de estadía solo están disponibles en modo demo.");
        }
        ReservaEstadia reserva = buscarReservaPropia(reservaId, correoCliente);
        if (reserva.getEstadoReserva() != EstadoReserva.CHECKED_IN) {
            throw new IllegalStateException("Solo puedes pedir tiempo extra después del check-in.");
        }
        if (reserva.getFechaFin().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La estadía ya terminó y no se puede ampliar.");
        }
        ModalidadTurno modalidad = modalidadTurnoRepository.findById(modalidadId)
                .orElseThrow(() -> new IllegalArgumentException("La duración seleccionada no existe."));
        Habitacion habitacion = habitacionRepository.findByIdForUpdate(reserva.getHabitacion().getId())
                .orElseThrow(() -> new IllegalStateException("La habitación de la reserva ya no existe."));
        LocalDateTime inicio = reserva.getFechaFin();
        LocalDateTime fin = calcularFin(inicio, modalidad);
        TarifaHabitacion tarifa = buscarTarifa(
                reserva.getTipoHabitacion().getId(), modalidad.getId(), inicio.toLocalDate());
        if (!habitacionDisponibleParaExtension(habitacion, reserva, inicio, fin)) {
            throw new IllegalStateException("La habitación ya está reservada después de tu horario.");
        }

        ExtensionEstadia extension = ExtensionEstadia.builder()
                .reserva(reserva)
                .modalidadTurno(modalidad)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .montoTotal(tarifa.getPrecioEstadiaBase())
                .metodoPago(MetodoPagoReserva.EFECTIVO)
                .estado(EstadoExtensionReserva.PENDIENTE_PAGO)
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(MINUTOS_PARA_PAGAR))
                .fechaCreacion(LocalDateTime.now())
                .build();
        return extensionEstadiaRepository.save(extension);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtensionEstadia> buscarExtensionCliente(Long extensionId,
                                                              String correoCliente) {
        return extensionEstadiaRepository
                .findByIdAndReservaClienteCorreoElectronicoIgnoreCase(extensionId, correoCliente);
    }

    @Override
    @Transactional
    public ExtensionEstadia simularPagoExtension(String correoCliente, Long extensionId,
                                                  MetodoPagoReserva metodoPago) {
        if (!demoMode) {
            throw new IllegalStateException("El pago simulado solo está disponible en modo demo.");
        }
        ExtensionEstadia extension = extensionEstadiaRepository
                .findByIdAndReservaClienteCorreoElectronicoIgnoreCase(extensionId, correoCliente)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la ampliación."));
        validarExtensionPendienteDePago(extension);
        ReservaEstadia reserva = extension.getReserva();
        if (reserva.getEstadoReserva() != EstadoReserva.CHECKED_IN) {
            throw new IllegalStateException("La estadía ya no está habilitada para ampliaciones.");
        }
        Habitacion habitacion = habitacionRepository.findByIdForUpdate(reserva.getHabitacion().getId())
                .orElseThrow(() -> new IllegalStateException("La habitación de la reserva ya no existe."));
        if (!habitacionDisponibleParaExtension(habitacion, reserva,
                extension.getFechaInicio(), extension.getFechaFin(), extension.getId())) {
            throw new IllegalStateException("La habitación ya está reservada después de tu horario.");
        }

        extension.setMetodoPago(metodoPago);
        extension.setEstado(EstadoExtensionReserva.CONFIRMADA);
        extension.setReservaTemporalHasta(null);
        reserva.setFechaFin(extension.getFechaFin());
        reserva.setMontoTotal(reserva.getMontoTotal().add(extension.getMontoTotal()));
        reservaEstadiaRepository.save(reserva);
        return extensionEstadiaRepository.save(extension);
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void expirarHoldsVencidos() {
        OffsetDateTime ahora = OffsetDateTime.now();
        reservaEstadiaRepository.expirarReservasPendientes(
                EstadoReserva.PENDIENTE_PAGO, EstadoReserva.EXPIRADA, ahora);
        extensionEstadiaRepository.expirarExtensionesPendientes(
                EstadoExtensionReserva.PENDIENTE_PAGO, EstadoExtensionReserva.EXPIRADA, ahora);
    }

    private ReservaEstadia confirmarPago(ReservaEstadia reserva, MetodoPagoReserva metodoPago) {
        if (reserva.getHabitacion() == null) {
            throw new IllegalStateException("La reserva no tiene una habitación asignada.");
        }
        reserva.setMetodoPago(metodoPago);
        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        reserva.setFechaPago(LocalDateTime.now());
        reserva.setReservaTemporalHasta(null);
        reserva.setTokenQr(UUID.randomUUID().toString());
        return reservaEstadiaRepository.save(reserva);
    }

    private ReservaEstadia buscarReservaPropia(Long reservaId, String correoCliente) {
        return reservaEstadiaRepository
                .findByIdAndClienteCorreoElectronicoIgnoreCase(reservaId, correoCliente)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró esa reserva."));
    }

    private void validarReservaPendienteDePago(ReservaEstadia reserva) {
        if (reserva.getEstadoReserva() != EstadoReserva.PENDIENTE_PAGO) {
            throw new IllegalStateException("La reserva ya no está pendiente de pago.");
        }
        if (reserva.getReservaTemporalHasta() != null
                && !reserva.getReservaTemporalHasta().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("El tiempo para pagar venció; la habitación quedó disponible.");
        }
    }

    private void validarExtensionPendienteDePago(ExtensionEstadia extension) {
        if (extension.getEstado() != EstadoExtensionReserva.PENDIENTE_PAGO) {
            throw new IllegalStateException("La ampliación ya no está pendiente de pago.");
        }
        if (!extension.getReservaTemporalHasta().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("El tiempo para pagar la ampliación venció.");
        }
    }

    private boolean estaDisponible(Habitacion habitacion, LocalDateTime inicio, LocalDateTime fin) {
        if (habitacion.getEstadoRack() == null
                || !(habitacion.getEstadoRack().equalsIgnoreCase("LIBRE")
                || habitacion.getEstadoRack().equalsIgnoreCase("DISPONIBLE"))) {
            return false;
        }
        return !tieneCruceActivo(habitacion.getId(), inicio, fin);
    }

    private boolean habitacionDisponibleParaExtension(Habitacion habitacion, ReservaEstadia reserva,
                                                       LocalDateTime inicio, LocalDateTime fin) {
        return habitacionDisponibleParaExtension(habitacion, reserva, inicio, fin, null);
    }

    private boolean habitacionDisponibleParaExtension(Habitacion habitacion, ReservaEstadia reserva,
                                                       LocalDateTime inicio, LocalDateTime fin,
                                                       Long extensionExcluir) {
        if (habitacion.getEstadoRack() == null
                || !(habitacion.getEstadoRack().equalsIgnoreCase("LIBRE")
                || habitacion.getEstadoRack().equalsIgnoreCase("DISPONIBLE"))) {
            return false;
        }
        if (reservaEstadiaRepository.existeCruceActivo(habitacion.getId(), inicio, fin,
                ESTADOS_QUE_OCUPAN_HABITACION, EstadoReserva.PENDIENTE_PAGO,
                OffsetDateTime.now())) {
            return false;
        }
        if (extensionExcluir == null) {
            return !extensionEstadiaRepository.existeCrucePendiente(
                    habitacion.getId(), inicio, fin, EstadoExtensionReserva.PENDIENTE_PAGO,
                    OffsetDateTime.now());
        }
        return !extensionEstadiaRepository.existeOtroCrucePendiente(
                extensionExcluir, habitacion.getId(), inicio, fin,
                EstadoExtensionReserva.PENDIENTE_PAGO, OffsetDateTime.now());
    }

    private boolean tieneCruceActivo(Integer habitacionId, LocalDateTime inicio, LocalDateTime fin) {
        return reservaEstadiaRepository.existeCruceActivo(habitacionId, inicio, fin,
                ESTADOS_QUE_OCUPAN_HABITACION, EstadoReserva.PENDIENTE_PAGO,
                OffsetDateTime.now())
                || extensionEstadiaRepository.existeCrucePendiente(
                habitacionId, inicio, fin, EstadoExtensionReserva.PENDIENTE_PAGO,
                OffsetDateTime.now());
    }

    private void validarDisponibilidad(Habitacion habitacion, LocalDateTime inicio,
                                       LocalDateTime fin) {
        if (!estaDisponible(habitacion, inicio, fin)) {
            throw new IllegalStateException("La habitación ya está ocupada en ese horario.");
        }
    }

    private void validarCapacidad(TipoHabitacion tipo, Integer cantidadPersonas) {
        if (cantidadPersonas == null || cantidadPersonas < 1
                || cantidadPersonas > tipo.getCapacidadMaxima()) {
            throw new IllegalArgumentException("La cantidad de personas supera la capacidad de la suite.");
        }
    }

    private LocalDateTime calcularInicio(LocalDate fechaReserva, java.time.LocalTime horaInicio) {
        if (fechaReserva == null || horaInicio == null) {
            throw new IllegalArgumentException("Selecciona la fecha y hora de ingreso.");
        }
        return fechaReserva.atTime(horaInicio);
    }

    private LocalDateTime calcularFin(LocalDateTime inicio, ModalidadTurno modalidad) {
        if (modalidad.getDuracionHoras() == null || modalidad.getDuracionHoras().signum() <= 0) {
            throw new IllegalStateException("La duración seleccionada no es válida.");
        }
        int minutos = modalidad.getDuracionHoras().multiply(BigDecimal.valueOf(60)).intValueExact();
        return inicio.plusMinutes(minutos);
    }

    private void validarFechaFutura(LocalDateTime inicio) {
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Selecciona una hora de ingreso futura.");
        }
    }

    private TarifaHabitacion buscarTarifa(Integer tipoId, Integer modalidadId, LocalDate fecha) {
        return tarifaHabitacionRepository.findByIdIdTipoHabitacion(tipoId).stream()
                .filter(tarifa -> tarifa.getId().getIdModalidad().equals(modalidadId))
                .filter(tarifa -> !fecha.isBefore(tarifa.getVigenciaDesde())
                        && !fecha.isAfter(tarifa.getVigenciaHasta()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una tarifa vigente para esa suite y duración en la fecha elegida."));
    }
}
