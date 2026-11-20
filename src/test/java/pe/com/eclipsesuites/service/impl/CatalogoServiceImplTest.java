package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dto.response.DisponibilidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteDetalleDTO;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.IReservaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogoServiceImplTest {

    @Mock
    private IReservaService reservaService;

    @Mock
    private HabitacionRepository habitacionRepository;

    @InjectMocks
    private CatalogoServiceImpl catalogoService;

    private final TipoHabitacion panoramica = TipoHabitacion.builder()
            .id(3)
            .codTipo("PANORAMICA")
            .nombreTematico("Suite Panorámica")
            .descripcionTematica("Vista panorámica al exterior.")
            .galeriaFotos("/img/suite-galaxia.webp")
            .capacidadMaxima((short) 2)
            .amenidades("Wi-Fi|Smart TV|Frigobar")
            .build();

    private final ModalidadTurno cuatroHoras = ModalidadTurno.builder()
            .id(1)
            .nombreTurno("4 horas")
            .duracionHoras(new BigDecimal("4.00"))
            .horaInicioFija(LocalTime.of(18, 0))
            .horaFinFija(LocalTime.of(22, 0))
            .permiteAmanecida(false)
            .build();

    private final ModalidadTurno seisHoras = ModalidadTurno.builder()
            .id(2)
            .nombreTurno("6 horas")
            .duracionHoras(new BigDecimal("6.00"))
            .horaInicioFija(LocalTime.of(18, 0))
            .horaFinFija(LocalTime.MIDNIGHT)
            .permiteAmanecida(true)
            .build();

    @Test
    void listarSuitesCalculaDisponibilidadOperativaYPrecioDesde() {
        configurarCatalogo();
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(8L);
        when(habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(3, "LIBRE"))
                .thenReturn(2L);
        when(habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(3, "DISPONIBLE"))
                .thenReturn(1L);

        List<SuiteCatalogoDTO> suites = catalogoService.listarSuites(null, null, null);

        assertThat(suites).hasSize(1);
        SuiteCatalogoDTO suite = suites.get(0);
        assertThat(suite.getNombre()).isEqualTo("Suite Panorámica");
        assertThat(suite.getHabitacionesTotales()).isEqualTo(8);
        assertThat(suite.getHabitacionesDisponibles()).isEqualTo(3);
        assertThat(suite.isDisponible()).isTrue();
        assertThat(suite.getPrecioDesde()).isEqualByComparingTo("120.00");
        assertThat(suite.getModalidadDesde()).isEqualTo("4 horas");
        assertThat(suite.getAmenidades()).containsExactly("Wi-Fi", "Smart TV", "Frigobar");
        assertThat(suite.getTarifas()).hasSize(2);
        verify(reservaService, never()).listarHabitacionesDisponibles(any(), any(), any());
    }

    @Test
    void listarSuitesConFechaYHoraUsaDisponibilidadReal() {
        configurarCatalogo();
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(8L);
        LocalDateTime inicio = LocalDateTime.of(2099, 6, 15, 18, 0);
        when(reservaService.listarHabitacionesDisponibles(3, inicio, inicio.plusHours(6)))
                .thenReturn(Set.of(27, 28));

        List<SuiteCatalogoDTO> suites = catalogoService.listarSuites(
                LocalDate.of(2099, 6, 15), LocalTime.of(18, 0), 2);

        assertThat(suites).hasSize(1);
        assertThat(suites.get(0).getHabitacionesDisponibles()).isEqualTo(2);
        assertThat(suites.get(0).isDisponible()).isTrue();
    }

    @Test
    void listarSuitesConModalidadFiltradaUsaElPrecioDeEsaDuracion() {
        configurarCatalogo();
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(8L);
        when(habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(eq(3), anyString()))
                .thenReturn(8L);

        SuiteCatalogoDTO suite = catalogoService.listarSuites(null, null, 2).get(0);

        assertThat(suite.getPrecioDesde()).isEqualByComparingTo("170.00");
        assertThat(suite.getModalidadDesde()).isEqualTo("6 horas");
    }

    @Test
    void listarSuitesSinTarifasDevuelvePrecioNuloYNoDisponible() {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(panoramica));
        when(reservaService.listarModalidades()).thenReturn(List.of(cuatroHoras));
        when(reservaService.listarTarifas()).thenReturn(List.of());
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(0L);
        when(habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(eq(3), anyString()))
                .thenReturn(0L);

        SuiteCatalogoDTO suite = catalogoService.listarSuites(null, null, null).get(0);

        assertThat(suite.getPrecioDesde()).isNull();
        assertThat(suite.getModalidadDesde()).isNull();
        assertThat(suite.getTarifas()).isEmpty();
        assertThat(suite.isDisponible()).isFalse();
    }

    @Test
    void obtenerDetalleMarcaHabitacionesDisponibles() {
        configurarCatalogo();
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(3L);
        when(habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(eq(3), anyString()))
                .thenReturn(2L);
        when(reservaService.listarHabitacionesPorTipo(3)).thenReturn(List.of(
                habitacion(27, "509", "LIBRE"),
                habitacion(28, "510", "OCUPADA"),
                habitacion(29, "512", "LIBRE")));

        Optional<SuiteDetalleDTO> detalle = catalogoService.obtenerDetalle(
                "panoramica", null, null, null);

        assertThat(detalle).isPresent();
        assertThat(detalle.get().getHabitaciones()).hasSize(3);
        assertThat(detalle.get().getHabitaciones())
                .extracting("numero", "disponible")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("509", true),
                        org.assertj.core.groups.Tuple.tuple("510", false),
                        org.assertj.core.groups.Tuple.tuple("512", true));
    }

    @Test
    void obtenerDetalleConHorarioFiltraPorHabitacionesDisponibles() {
        configurarCatalogo();
        when(habitacionRepository.countByTipoHabitacionId(3)).thenReturn(3L);
        LocalDateTime inicio = LocalDateTime.of(2099, 6, 15, 18, 0);
        when(reservaService.listarHabitacionesDisponibles(3, inicio, inicio.plusHours(4)))
                .thenReturn(Set.of(29));
        when(reservaService.listarHabitacionesPorTipo(3)).thenReturn(List.of(
                habitacion(27, "509", "LIBRE"),
                habitacion(29, "512", "LIBRE")));

        SuiteDetalleDTO detalle = catalogoService.obtenerDetalle("PANORAMICA",
                LocalDate.of(2099, 6, 15), LocalTime.of(18, 0), 1).orElseThrow();

        assertThat(detalle.getHabitaciones())
                .extracting("numero", "disponible")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("509", false),
                        org.assertj.core.groups.Tuple.tuple("512", true));
        assertThat(detalle.getHabitacionesDisponibles()).isEqualTo(1);
    }

    @Test
    void obtenerDetalleDevuelveVacioSiLaSuiteNoExiste() {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(panoramica));

        assertThat(catalogoService.obtenerDetalle("NO_EXISTE", null, null, null)).isEmpty();
        assertThat(catalogoService.obtenerDetalle(" ", null, null, null)).isEmpty();
    }

    @Test
    void consultarDisponibilidadDevuelveNumerosDeHabitacionLibres() {
        configurarCatalogo();
        when(reservaService.listarHabitacionesPorTipo(3)).thenReturn(List.of(
                habitacion(27, "509", "LIBRE"),
                habitacion(28, "510", "OCUPADA"),
                habitacion(29, "512", "LIBRE")));
        LocalDateTime inicio = LocalDateTime.of(2099, 6, 15, 18, 0);
        when(reservaService.listarHabitacionesDisponibles(3, inicio, inicio.plusHours(4)))
                .thenReturn(Set.of(29, 27));

        DisponibilidadCatalogoDTO disponibilidad = catalogoService.consultarDisponibilidad(
                3, LocalDate.of(2099, 6, 15), LocalTime.of(18, 0), 1);

        assertThat(disponibilidad.getModalidad()).isEqualTo("4 horas");
        assertThat(disponibilidad.getHoraFin()).isEqualTo(LocalTime.of(22, 0));
        assertThat(disponibilidad.getHabitacionesDisponibles()).isEqualTo(2);
        assertThat(disponibilidad.getNumerosHabitacion()).containsExactly("509", "512");
        assertThat(disponibilidad.getIdsHabitacion()).containsExactly(27, 29);
        assertThat(disponibilidad.isDisponible()).isTrue();
    }

    @Test
    void consultarDisponibilidadUsaLaPrimeraModalidadSiNoSeIndica() {
        configurarCatalogo();
        when(reservaService.listarHabitacionesPorTipo(3)).thenReturn(List.of());
        LocalDateTime inicio = LocalDateTime.of(2099, 6, 15, 18, 0);
        when(reservaService.listarHabitacionesDisponibles(3, inicio, inicio.plusHours(4)))
                .thenReturn(Set.of());

        DisponibilidadCatalogoDTO disponibilidad = catalogoService.consultarDisponibilidad(
                3, LocalDate.of(2099, 6, 15), LocalTime.of(18, 0), null);

        assertThat(disponibilidad.getModalidad()).isEqualTo("4 horas");
        assertThat(disponibilidad.isDisponible()).isFalse();
        assertThat(disponibilidad.getNumerosHabitacion()).isEmpty();
    }

    @Test
    void consultarDisponibilidadExigeFechaYHora() {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(panoramica));

        assertThatThrownBy(() -> catalogoService.consultarDisponibilidad(3, null, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha y hora");
    }

    @Test
    void consultarDisponibilidadFallaSiLaSuiteNoExiste() {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(panoramica));

        assertThatThrownBy(() -> catalogoService.consultarDisponibilidad(99,
                LocalDate.of(2099, 6, 15), LocalTime.of(18, 0), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    private void configurarCatalogo() {
        when(reservaService.listarTiposHabitacion()).thenReturn(List.of(panoramica));
        when(reservaService.listarModalidades()).thenReturn(List.of(cuatroHoras, seisHoras));
        when(reservaService.listarTarifas()).thenReturn(List.of(
                tarifa(3, cuatroHoras, "120.00"),
                tarifa(3, seisHoras, "170.00")));
    }

    private TarifaHabitacion tarifa(Integer tipoId, ModalidadTurno modalidad, String precio) {
        return TarifaHabitacion.builder()
                .id(new TarifaHabitacionPK(tipoId, modalidad.getId()))
                .tipoHabitacion(panoramica)
                .modalidadTurno(modalidad)
                .precioEstadiaBase(new BigDecimal(precio))
                .costoHoraAdicional(BigDecimal.ZERO)
                .esTarifaFinSemana(false)
                .vigenciaDesde(LocalDate.now().minusDays(1))
                .vigenciaHasta(LocalDate.now().plusYears(1))
                .build();
    }

    private Habitacion habitacion(Integer id, String numero, String estado) {
        return Habitacion.builder()
                .id(id)
                .tipoHabitacion(panoramica)
                .numHabitacion(numero)
                .piso((short) 5)
                .estadoRack(estado)
                .build();
    }
}
