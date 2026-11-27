package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.eclipsesuites.dao.ReservaEstadiaRepository;
import pe.com.eclipsesuites.dto.response.BoletaReservaDTO;
import pe.com.eclipsesuites.dto.response.PerfilClienteResponseDTO;
import pe.com.eclipsesuites.dto.response.ReservaResumenDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.MetodoPagoReserva;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.ReservaEstadia;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IClienteAuthService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientePerfilServiceImplTest {

    private static final String CORREO = "ana@example.com";

    @Mock
    private IClienteAuthService clienteAuthService;

    @Mock
    private ReservaEstadiaRepository reservaEstadiaRepository;

    private ClientePerfilServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new ClientePerfilServiceImpl(clienteAuthService, reservaEstadiaRepository);
    }

    @Test
    void obtenerPerfil_incluyeReservaActivaYTotalDeReservas() {
        when(clienteAuthService.buscarPorCorreo(CORREO)).thenReturn(cliente());
        when(reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(CORREO))
                .thenReturn(List.of(reserva(EstadoReserva.CONFIRMADA), reserva(EstadoReserva.CANCELADA)));

        PerfilClienteResponseDTO perfil = servicio.obtenerPerfil(CORREO);

        assertEquals("Ana Rojas", perfil.nombreCompleto());
        assertTrue(perfil.activo());
        assertEquals(2, perfil.totalReservas());
        assertNotNull(perfil.reservaActiva());
        assertEquals("Confirmada", perfil.reservaActiva().estadoEtiqueta());
        assertEquals("Suite Galaxia", perfil.reservaActiva().suite());
        assertEquals("509", perfil.reservaActiva().habitacion());
        assertEquals("/img/galaxia.webp", perfil.reservaActiva().imagen());
        assertFalse(perfil.fechaRegistro().isBlank());
    }

    @Test
    void obtenerPerfil_noMarcaReservaActivaSiSoloHayCanceladas() {
        when(clienteAuthService.buscarPorCorreo(CORREO)).thenReturn(cliente());
        when(reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(CORREO))
                .thenReturn(List.of(reserva(EstadoReserva.CANCELADA)));

        PerfilClienteResponseDTO perfil = servicio.obtenerPerfil(CORREO);

        assertNull(perfil.reservaActiva());
        assertEquals(1, perfil.totalReservas());
    }

    @Test
    void obtenerPerfil_pagoPendienteFueraDeVentanaNoCuentaComoActiva() {
        ReservaEstadia vencida = reserva(EstadoReserva.PENDIENTE_PAGO);
        vencida.setReservaTemporalHasta(OffsetDateTime.now().minusMinutes(5));

        when(clienteAuthService.buscarPorCorreo(CORREO)).thenReturn(cliente());
        when(reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(CORREO))
                .thenReturn(List.of(vencida));

        assertNull(servicio.obtenerPerfil(CORREO).reservaActiva());
    }

    @Test
    void listarReservas_ordenaDesdeElRepositorioYSeparaImportes() {
        when(clienteAuthService.buscarPorCorreo(CORREO)).thenReturn(cliente());
        when(reservaEstadiaRepository
                .findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(CORREO))
                .thenReturn(List.of(reserva(EstadoReserva.CONFIRMADA)));

        List<ReservaResumenDTO> reservas = servicio.listarReservas(CORREO);

        assertEquals(1, reservas.size());
        assertEquals("Reserva #" + reservas.get(0).id(), reservas.get(0).codigo());
        assertEquals(new BigDecimal("118.00"), reservas.get(0).montoTotal());
        assertEquals("Confirmada", reservas.get(0).estadoEtiqueta());
        assertEquals("4 horas", reservas.get(0).modalidad());
        assertEquals("Yape", reservas.get(0).metodoPago());
        assertEquals("15/06/2099 18:00", reservas.get(0).fechaInicio());
    }

    @Test
    void obtenerBoleta_desglosaIgvYGeneraNumero() {
        when(reservaEstadiaRepository.findByIdAndClienteCorreoElectronicoIgnoreCase(17L, CORREO))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA)));

        BoletaReservaDTO boleta = servicio.obtenerBoleta(CORREO, 17L);

        assertEquals("001-0000017", boleta.numeroBoleta());
        assertEquals(new BigDecimal("100.00"), boleta.subtotal());
        assertEquals(new BigDecimal("18.00"), boleta.igv());
        assertEquals(new BigDecimal("118.00"), boleta.total());
        assertEquals("Ana Rojas", boleta.cliente());
        assertEquals("ECLIPSE SUITES HOTEL", boleta.hotel());
    }

    @Test
    void obtenerBoleta_reservaDeOtroClienteSeRechaza() {
        when(reservaEstadiaRepository.findByIdAndClienteCorreoElectronicoIgnoreCase(17L, CORREO))
                .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerBoleta(CORREO, 17L));

        assertEquals("Reserva no encontrada", error.getMessage());
    }

    private Cliente cliente() {
        return Cliente.builder()
                .id(1)
                .nombre("Ana")
                .apellido("Rojas")
                .correoElectronico(CORREO)
                .tipoDocumento("DNI")
                .numDocumento("12345678")
                .paisEmisionDoc("PER")
                .telefono("+51999123456")
                .edad(30)
                .activo(true)
                .fechaAceptacionPrivacidad(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private ReservaEstadia reserva(EstadoReserva estado) {
        return ReservaEstadia.builder()
                .id(17L)
                .cliente(cliente())
                .tipoHabitacion(TipoHabitacion.builder()
                        .nombreTematico("Suite Galaxia")
                        .galeriaFotos("/img/galaxia.webp,/img/galaxia-2.webp")
                        .build())
                .habitacion(Habitacion.builder().numHabitacion("509").build())
                .modalidadTurno(ModalidadTurno.builder().nombreTurno("4 horas").build())
                .fechaInicio(LocalDateTime.of(2099, 6, 15, 18, 0))
                .fechaFin(LocalDateTime.of(2099, 6, 15, 22, 0))
                .fechaCreacion(LocalDateTime.of(2099, 6, 10, 9, 30))
                .cantidadPersonas(2)
                .montoTotal(new BigDecimal("118.00"))
                .metodoPago(MetodoPagoReserva.YAPE)
                .estadoReserva(estado)
                .reservaTemporalHasta(OffsetDateTime.now().plusMinutes(15))
                .build();
    }
}
