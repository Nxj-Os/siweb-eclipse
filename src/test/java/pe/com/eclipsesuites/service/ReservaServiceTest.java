package pe.com.eclipsesuites.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dao.ExtensionEstadiaRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.ModalidadTurnoRepository;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dao.TarifaHabitacionRepository;
import pe.com.eclipsesuites.dao.TipoHabitacionRepository;
import pe.com.eclipsesuites.dto.request.ReservaRequestDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.EstadoExtensionReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.impl.ReservaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDate FECHA_FUTURA = LocalDate.of(2099, 6, 15);

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TipoHabitacionRepository tipoHabitacionRepository;
    @Mock
    private ModalidadTurnoRepository modalidadTurnoRepository;
    @Mock
    private HabitacionRepository habitacionRepository;
    @Mock
    private TarifaHabitacionRepository tarifaHabitacionRepository;
    @Mock
    private ReservaEstadiaRepository reservaEstadiaRepository;
    @Mock
    private ExtensionEstadiaRepository extensionEstadiaRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservaService, "demoMode", true);
    }

    @Test
    void crearReservaRetieneHabitacionFisicaCincoMinutosConTarifaVigente() {
        Cliente cliente = Cliente.builder().correoElectronico("ana@example.com").build();
        TipoHabitacion tipo = TipoHabitacion.builder().id(3).capacidadMaxima((short) 2).build();
        ModalidadTurno modalidad = modalidad();
        Habitacion habitacion = Habitacion.builder()
                .id(27)
                .tipoHabitacion(tipo)
                .estadoRack("LIBRE")
                .build();
        TarifaHabitacion tarifa = TarifaHabitacion.builder()
                .id(new TarifaHabitacionPK(3, 5))
                .precioEstadiaBase(new BigDecimal("125.00"))
                .vigenciaDesde(FECHA_FUTURA.minusDays(1))
                .vigenciaHasta(FECHA_FUTURA.plusDays(1))
                .build();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(cliente));
        when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
        when(modalidadTurnoRepository.findById(5)).thenReturn(Optional.of(modalidad));
        when(habitacionRepository.findByIdForUpdate(27)).thenReturn(Optional.of(habitacion));
        when(tarifaHabitacionRepository.findByIdIdTipoHabitacion(3)).thenReturn(List.of(tarifa));
        when(reservaEstadiaRepository.existeCruceActivo(
                eq(27), any(LocalDateTime.class), any(LocalDateTime.class), any(),
                eq(EstadoReserva.PENDIENTE_PAGO), any(OffsetDateTime.class))).thenReturn(false);
        when(extensionEstadiaRepository.existeCrucePendiente(
                eq(27), any(LocalDateTime.class), any(LocalDateTime.class), any(),
                any(OffsetDateTime.class))).thenReturn(false);
        when(reservaEstadiaRepository.save(any(ReservaEstadia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservaEstadia reserva = reservaService.crearReserva("ana@example.com",
                request(3, 27, 5, FECHA_FUTURA, 2));

        assertEquals(EstadoReserva.PENDIENTE_PAGO, reserva.getEstadoReserva());
        assertEquals(MetodoPagoReserva.EFECTIVO, reserva.getMetodoPago());
        assertEquals(new BigDecimal("125.00"), reserva.getMontoTotal());
        assertSame(tipo, reserva.getTipoHabitacion());
        assertSame(habitacion, reserva.getHabitacion());
        assertNotNull(reserva.getReservaTemporalHasta());
        assertEquals(true, reserva.getReservaTemporalHasta().isAfter(OffsetDateTime.now()));
        verify(reservaEstadiaRepository).save(reserva);
    }

    @Test
    void crearReservaRechazaCantidadMayorALaCapacidad() {
        TipoHabitacion tipo = TipoHabitacion.builder().id(3).capacidadMaxima((short) 2).build();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(Cliente.builder().correoElectronico("ana@example.com").build()));
        when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
        when(modalidadTurnoRepository.findById(5)).thenReturn(Optional.of(modalidad()));
        when(habitacionRepository.findByIdForUpdate(27)).thenReturn(Optional.of(
                Habitacion.builder().id(27).tipoHabitacion(tipo).estadoRack("LIBRE").build()));

        assertThrows(IllegalArgumentException.class, () -> reservaService.crearReserva(
                "ana@example.com", request(3, 27, 5, FECHA_FUTURA, 3)));
    }

    @Test
    void pagoDeDemoConfirmaReservaYLimpiaRetencion() {
        ReservaEstadia reserva = ReservaEstadia.builder()
                .id(11L)
                .cliente(Cliente.builder().correoElectronico("ana@example.com").build())
                .habitacion(Habitacion.builder().id(27).build())
                .estadoReserva(EstadoReserva.PENDIENTE_PAGO)
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(4))
                .build();
        when(reservaEstadiaRepository.findByIdAndClienteCorreoElectronicoIgnoreCase(
                11L, "ana@example.com")).thenReturn(Optional.of(reserva));
        when(reservaEstadiaRepository.save(reserva)).thenReturn(reserva);

        ReservaEstadia pagada = reservaService.simularPagoReserva(
                "ana@example.com", 11L, MetodoPagoReserva.YAPE);

        assertEquals(EstadoReserva.CONFIRMADA, pagada.getEstadoReserva());
        assertEquals(MetodoPagoReserva.YAPE, pagada.getMetodoPago());
        assertNotNull(pagada.getFechaPago());
        assertNotNull(pagada.getTokenQr());
        org.junit.jupiter.api.Assertions.assertNull(pagada.getReservaTemporalHasta());
    }

    @Test
    void recepcionConfirmaPagoYGeneraCodigoQr() {
        ReservaEstadia reserva = ReservaEstadia.builder()
                .id(11L)
                .tipoHabitacion(TipoHabitacion.builder().id(3).build())
                .habitacion(Habitacion.builder().id(27).build())
                .fechaInicio(FECHA_FUTURA.atTime(18, 0))
                .fechaFin(FECHA_FUTURA.atTime(22, 0))
                .metodoPago(MetodoPagoReserva.EFECTIVO)
                .estadoReserva(EstadoReserva.PENDIENTE_PAGO)
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(4))
                .build();
        when(reservaEstadiaRepository.findById(11L)).thenReturn(Optional.of(reserva));
        when(reservaEstadiaRepository.save(reserva)).thenReturn(reserva);

        ReservaEstadia actualizada = reservaService.actualizarEstadoPendiente(
                11L, EstadoReserva.CONFIRMADA);

        assertEquals(EstadoReserva.CONFIRMADA, actualizada.getEstadoReserva());
        assertNotNull(actualizada.getTokenQr());
        verify(reservaEstadiaRepository).save(reserva);
    }

    @Test
    void ampliacionRetieneLaMismaHabitacionDesdeLaHoraDeSalida() {
        TipoHabitacion tipo = TipoHabitacion.builder().id(3).build();
        Habitacion habitacion = Habitacion.builder()
                .id(27)
                .tipoHabitacion(tipo)
                .estadoRack("LIBRE")
                .build();
        ReservaEstadia reserva = ReservaEstadia.builder()
                .id(11L)
                .cliente(Cliente.builder().correoElectronico("ana@example.com").build())
                .tipoHabitacion(tipo)
                .habitacion(habitacion)
                .fechaInicio(FECHA_FUTURA.atTime(18, 0))
                .fechaFin(FECHA_FUTURA.atTime(22, 0))
                .montoTotal(new BigDecimal("125.00"))
                .estadoReserva(EstadoReserva.CHECKED_IN)
                .build();
        TarifaHabitacion tarifa = TarifaHabitacion.builder()
                .id(new TarifaHabitacionPK(3, 5))
                .precioEstadiaBase(new BigDecimal("80.00"))
                .vigenciaDesde(FECHA_FUTURA.minusDays(1))
                .vigenciaHasta(FECHA_FUTURA.plusDays(1))
                .build();
        when(reservaEstadiaRepository.findByIdAndClienteCorreoElectronicoIgnoreCase(
                11L, "ana@example.com")).thenReturn(Optional.of(reserva));
        when(modalidadTurnoRepository.findById(5)).thenReturn(Optional.of(modalidad()));
        when(habitacionRepository.findByIdForUpdate(27)).thenReturn(Optional.of(habitacion));
        when(tarifaHabitacionRepository.findByIdIdTipoHabitacion(3)).thenReturn(List.of(tarifa));
        when(reservaEstadiaRepository.existeCruceActivo(
                eq(27), any(LocalDateTime.class), any(LocalDateTime.class), any(),
                eq(EstadoReserva.PENDIENTE_PAGO), any(OffsetDateTime.class))).thenReturn(false);
        when(extensionEstadiaRepository.existeCrucePendiente(
                eq(27), any(LocalDateTime.class), any(LocalDateTime.class), any(),
                any(OffsetDateTime.class))).thenReturn(false);
        when(extensionEstadiaRepository.save(any(ExtensionEstadia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExtensionEstadia extension = reservaService.crearExtension(
                "ana@example.com", 11L, 5);

        assertEquals(EstadoExtensionReserva.PENDIENTE_PAGO, extension.getEstado());
        assertEquals(FECHA_FUTURA.atTime(22, 0), extension.getFechaInicio());
        assertEquals(FECHA_FUTURA.plusDays(1).atTime(2, 0), extension.getFechaFin());
        assertEquals(new BigDecimal("80.00"), extension.getMontoTotal());
    }

    private ReservaRequestDTO request(Integer tipoId, Integer habitacionId, Integer modalidadId,
                                      LocalDate fecha, Integer personas) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setTipoHabitacionId(tipoId);
        request.setHabitacionId(habitacionId);
        request.setModalidadId(modalidadId);
        request.setFechaReserva(fecha);
        request.setHoraInicio(LocalTime.of(18, 0));
        request.setCantidadPersonas(personas);
        return request;
    }

    private ModalidadTurno modalidad() {
        return ModalidadTurno.builder()
                .id(5)
                .nombreTurno("4 horas")
                .duracionHoras(new BigDecimal("4.00"))
                .horaInicioFija(LocalTime.of(18, 0))
                .horaFinFija(LocalTime.of(22, 0))
                .permiteAmanecida(false)
                .build();
    }
}
