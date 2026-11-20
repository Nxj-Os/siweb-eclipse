package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dto.response.DisponibilidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.HabitacionCatalogoDTO;
import pe.com.eclipsesuites.dto.response.ModalidadCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteCatalogoDTO;
import pe.com.eclipsesuites.dto.response.SuiteDetalleDTO;
import pe.com.eclipsesuites.dto.response.TarifaCatalogoDTO;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TipoHabitacion;
import pe.com.eclipsesuites.service.ICatalogoService;
import pe.com.eclipsesuites.service.IReservaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements ICatalogoService {

    private static final String ESTADO_LIBRE = "LIBRE";
    private static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    private static final String SEPARADOR_AMENIDADES = "\\|";
    private static final String IMAGEN_POR_DEFECTO = "/img/suite-placeholder.svg";

    private final IReservaService reservaService;
    private final HabitacionRepository habitacionRepository;

    @Override
    public List<SuiteCatalogoDTO> listarSuites(LocalDate fecha, LocalTime hora,
                                               Integer modalidadId) {
        List<ModalidadTurno> modalidades = reservaService.listarModalidades();
        List<TarifaHabitacion> tarifas = reservaService.listarTarifas();
        LocalDateTime inicio = resolverInicio(fecha, hora);
        LocalDateTime fin = resolverFin(inicio, modalidadId, modalidades);

        return reservaService.listarTiposHabitacion().stream()
                .map(tipo -> construirSuite(tipo, tarifas, inicio, fin, modalidadId))
                .sorted(Comparator.comparing(SuiteCatalogoDTO::getNombre,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<SuiteDetalleDTO> obtenerDetalle(String codTipo, LocalDate fecha,
                                                    LocalTime hora, Integer modalidadId) {
        if (codTipo == null || codTipo.isBlank()) {
            return Optional.empty();
        }
        Optional<TipoHabitacion> tipoEncontrado = reservaService.listarTiposHabitacion().stream()
                .filter(tipo -> tipo.getCodTipo() != null
                        && tipo.getCodTipo().equalsIgnoreCase(codTipo.trim()))
                .findFirst();
        if (tipoEncontrado.isEmpty()) {
            return Optional.empty();
        }

        TipoHabitacion tipo = tipoEncontrado.get();
        List<ModalidadTurno> modalidades = reservaService.listarModalidades();
        LocalDateTime inicio = resolverInicio(fecha, hora);
        LocalDateTime fin = resolverFin(inicio, modalidadId, modalidades);
        SuiteCatalogoDTO resumen = construirSuite(tipo, reservaService.listarTarifas(), inicio,
                fin, modalidadId);

        Set<Integer> disponibles = inicio != null && fin != null
                ? reservaService.listarHabitacionesDisponibles(tipo.getId(), inicio, fin)
                : null;
        List<HabitacionCatalogoDTO> habitaciones = reservaService
                .listarHabitacionesPorTipo(tipo.getId()).stream()
                .map(habitacion -> construirHabitacion(habitacion, disponibles))
                .toList();

        return Optional.of(SuiteDetalleDTO.builder()
                .id(resumen.getId())
                .codTipo(resumen.getCodTipo())
                .nombre(resumen.getNombre())
                .descripcion(resumen.getDescripcion())
                .imagen(resumen.getImagen())
                .capacidadMaxima(resumen.getCapacidadMaxima())
                .habitacionesTotales(resumen.getHabitacionesTotales())
                .habitacionesDisponibles(resumen.getHabitacionesDisponibles())
                .disponible(resumen.isDisponible())
                .precioDesde(resumen.getPrecioDesde())
                .modalidadDesde(resumen.getModalidadDesde())
                .amenidades(resumen.getAmenidades())
                .tarifas(resumen.getTarifas())
                .habitaciones(habitaciones)
                .build());
    }

    @Override
    public DisponibilidadCatalogoDTO consultarDisponibilidad(Integer tipoHabitacionId,
                                                             LocalDate fecha, LocalTime hora,
                                                             Integer modalidadId) {
        TipoHabitacion tipo = reservaService.listarTiposHabitacion().stream()
                .filter(item -> item.getId().equals(tipoHabitacionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La suite seleccionada no existe."));
        if (fecha == null || hora == null) {
            throw new IllegalArgumentException("Selecciona la fecha y hora de ingreso.");
        }

        List<ModalidadTurno> modalidades = reservaService.listarModalidades();
        ModalidadTurno modalidad = resolverModalidad(modalidadId, modalidades)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay modalidades de turno configuradas."));
        LocalDateTime inicio = fecha.atTime(hora);
        LocalDateTime fin = inicio.plusMinutes(minutosDe(modalidad));

        Set<Integer> disponibles = reservaService.listarHabitacionesDisponibles(
                tipo.getId(), inicio, fin);
        List<Habitacion> habitaciones = reservaService.listarHabitacionesPorTipo(tipo.getId());
        List<String> numeros = habitaciones.stream()
                .filter(habitacion -> disponibles.contains(habitacion.getId()))
                .map(Habitacion::getNumHabitacion)
                .toList();

        return DisponibilidadCatalogoDTO.builder()
                .tipoHabitacionId(tipo.getId())
                .codTipo(tipo.getCodTipo())
                .nombre(tipo.getNombreTematico())
                .fecha(fecha)
                .horaInicio(inicio.toLocalTime())
                .horaFin(fin.toLocalTime())
                .modalidadId(modalidad.getId())
                .modalidad(modalidad.getNombreTurno())
                .habitacionesTotales(habitaciones.size())
                .habitacionesDisponibles(disponibles.size())
                .disponible(!disponibles.isEmpty())
                .idsHabitacion(disponibles.stream().sorted().toList())
                .numerosHabitacion(numeros.stream().sorted().toList())
                .build();
    }

    @Override
    public List<ModalidadCatalogoDTO> listarModalidades() {
        return reservaService.listarModalidades().stream()
                .map(modalidad -> ModalidadCatalogoDTO.builder()
                        .id(modalidad.getId())
                        .nombre(modalidad.getNombreTurno())
                        .duracionHoras(modalidad.getDuracionHoras())
                        .build())
                .toList();
    }

    private SuiteCatalogoDTO construirSuite(TipoHabitacion tipo, List<TarifaHabitacion> tarifas,
                                            LocalDateTime inicio, LocalDateTime fin,
                                            Integer modalidadId) {
        int total = (int) habitacionRepository.countByTipoHabitacionId(tipo.getId());
        int disponibles = inicio != null && fin != null
                ? reservaService.listarHabitacionesDisponibles(tipo.getId(), inicio, fin).size()
                : contarHabitacionesLibres(tipo.getId());

        List<TarifaCatalogoDTO> tarifasSuite = tarifas.stream()
                .filter(tarifa -> tarifa.getId() != null
                        && tipo.getId().equals(tarifa.getId().getIdTipoHabitacion()))
                .map(this::construirTarifa)
                .sorted(Comparator.comparing(TarifaCatalogoDTO::getDuracionHoras,
                        Comparator.nullsLast(BigDecimal::compareTo)))
                .toList();

        List<TarifaCatalogoDTO> vigentes = tarifasSuite.stream()
                .filter(TarifaCatalogoDTO::isVigente)
                .toList();
        List<TarifaCatalogoDTO> base = vigentes.isEmpty() ? tarifasSuite : vigentes;
        // Si el visitante filtró por una duración concreta, el "desde" debe
        // corresponder a esa modalidad para no mostrar un precio incoherente.
        if (modalidadId != null) {
            List<TarifaCatalogoDTO> deLaModalidad = base.stream()
                    .filter(tarifa -> modalidadId.equals(tarifa.getModalidadId()))
                    .toList();
            if (!deLaModalidad.isEmpty()) {
                base = deLaModalidad;
            }
        }
        TarifaCatalogoDTO menor = base.stream()
                .filter(tarifa -> tarifa.getPrecio() != null)
                .min(Comparator.comparing(TarifaCatalogoDTO::getPrecio))
                .orElse(null);

        return SuiteCatalogoDTO.builder()
                .id(tipo.getId())
                .codTipo(tipo.getCodTipo())
                .nombre(tipo.getNombreTematico())
                .descripcion(tipo.getDescripcionTematica())
                .imagen(imagen(tipo))
                .capacidadMaxima(tipo.getCapacidadMaxima())
                .habitacionesTotales(total)
                .habitacionesDisponibles(disponibles)
                .disponible(disponibles > 0)
                .precioDesde(menor != null ? menor.getPrecio() : null)
                .modalidadDesde(menor != null ? menor.getModalidad() : null)
                .amenidades(separarAmenidades(tipo.getAmenidades()))
                .tarifas(tarifasSuite)
                .build();
    }

    private TarifaCatalogoDTO construirTarifa(TarifaHabitacion tarifa) {
        LocalDate hoy = LocalDate.now();
        boolean vigente = tarifa.getVigenciaDesde() == null || tarifa.getVigenciaHasta() == null
                || (!hoy.isBefore(tarifa.getVigenciaDesde())
                && !hoy.isAfter(tarifa.getVigenciaHasta()));
        ModalidadTurno modalidad = tarifa.getModalidadTurno();
        return TarifaCatalogoDTO.builder()
                .modalidadId(tarifa.getId() != null ? tarifa.getId().getIdModalidad() : null)
                .modalidad(modalidad != null ? modalidad.getNombreTurno() : null)
                .duracionHoras(modalidad != null ? modalidad.getDuracionHoras() : null)
                .precio(tarifa.getPrecioEstadiaBase())
                .costoHoraAdicional(tarifa.getCostoHoraAdicional())
                .vigente(vigente)
                .build();
    }

    private HabitacionCatalogoDTO construirHabitacion(Habitacion habitacion,
                                                      Set<Integer> disponibles) {
        boolean libre = esEstadoLibre(habitacion.getEstadoRack());
        boolean disponible = disponibles != null
                ? disponibles.contains(habitacion.getId())
                : libre;
        return HabitacionCatalogoDTO.builder()
                .id(habitacion.getId())
                .numero(habitacion.getNumHabitacion())
                .piso(habitacion.getPiso())
                .estadoRack(habitacion.getEstadoRack())
                .disponible(disponible)
                .build();
    }

    private int contarHabitacionesLibres(Integer tipoId) {
        long libres = habitacionRepository.countByTipoHabitacionIdAndEstadoRackIgnoreCase(
                tipoId, ESTADO_LIBRE);
        long disponibles = habitacionRepository
                .countByTipoHabitacionIdAndEstadoRackIgnoreCase(tipoId, ESTADO_DISPONIBLE);
        return (int) (libres + disponibles);
    }

    private LocalDateTime resolverInicio(LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            return null;
        }
        return fecha.atTime(hora);
    }

    private LocalDateTime resolverFin(LocalDateTime inicio, Integer modalidadId,
                                      List<ModalidadTurno> modalidades) {
        if (inicio == null) {
            return null;
        }
        return resolverModalidad(modalidadId, modalidades)
                .map(modalidad -> inicio.plusMinutes(minutosDe(modalidad)))
                .orElse(null);
    }

    private Optional<ModalidadTurno> resolverModalidad(Integer modalidadId,
                                                       List<ModalidadTurno> modalidades) {
        if (modalidades == null || modalidades.isEmpty()) {
            return Optional.empty();
        }
        if (modalidadId == null) {
            return Optional.of(modalidades.get(0));
        }
        return modalidades.stream()
                .filter(modalidad -> modalidad.getId().equals(modalidadId))
                .findFirst();
    }

    private int minutosDe(ModalidadTurno modalidad) {
        if (modalidad.getDuracionHoras() == null) {
            throw new IllegalStateException("La modalidad no tiene duración configurada.");
        }
        return modalidad.getDuracionHoras().multiply(BigDecimal.valueOf(60)).intValueExact();
    }

    private boolean esEstadoLibre(String estado) {
        if (estado == null) {
            return false;
        }
        String normalizado = estado.toUpperCase(Locale.ROOT);
        return ESTADO_LIBRE.equals(normalizado) || ESTADO_DISPONIBLE.equals(normalizado);
    }

    private String imagen(TipoHabitacion tipo) {
        return tipo.getGaleriaFotos() == null || tipo.getGaleriaFotos().isBlank()
                ? IMAGEN_POR_DEFECTO
                : tipo.getGaleriaFotos();
    }

    private List<String> separarAmenidades(String amenidades) {
        if (amenidades == null || amenidades.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenidades.split(SEPARADOR_AMENIDADES))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
