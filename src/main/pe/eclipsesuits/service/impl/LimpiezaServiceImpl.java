package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.EmpleadoRepository;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.OrdenLimpiezaRepository;
import pe.com.eclipsesuites.dao.ReporteAveriaRepository;
import pe.com.eclipsesuites.dto.request.AveriaRequestDTO;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.OrdenLimpieza;
import pe.com.eclipsesuites.entity.ReporteAveria;
import pe.com.eclipsesuites.service.ILimpiezaService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LimpiezaServiceImpl implements ILimpiezaService {

    private static final String ESTADO_SUCIA = "SUCIA";
    private static final String ESTADO_EN_LIMPIEZA = "EN_LIMPIEZA";
    private static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    private static final String ESTADO_MANTENIMIENTO = "MANTENIMIENTO";

    private static final String ORDEN_EN_LIMPIEZA = "EN_LIMPIEZA";
    private static final String ORDEN_LIMPIA = "LIMPIA";

    private static final List<String> GRAVEDADES_BLOQUEANTES = List.of("ALTA", "CRITICA");

    private final HabitacionRepository habitacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final OrdenLimpiezaRepository ordenLimpiezaRepository;
    private final ReporteAveriaRepository reporteAveriaRepository;

    public LimpiezaServiceImpl(HabitacionRepository habitacionRepository,
                                EmpleadoRepository empleadoRepository,
                                OrdenLimpiezaRepository ordenLimpiezaRepository,
                                ReporteAveriaRepository reporteAveriaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.empleadoRepository = empleadoRepository;
        this.ordenLimpiezaRepository = ordenLimpiezaRepository;
        this.reporteAveriaRepository = reporteAveriaRepository;
    }

    @Override
    public List<Habitacion> listarHabitacionesPendientes() {
        return habitacionRepository.findByEstadoRack(ESTADO_SUCIA);
    }

    @Override
    @Transactional
    public OrdenLimpieza iniciarLimpieza(Integer idHabitacion, Integer idEmpleado) {
        Habitacion habitacion = habitacionRepository.findById(idHabitacion)
                .orElseThrow(() -> new NoSuchElementException("Habitación no encontrada: " + idHabitacion));
        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado: " + idEmpleado));

        if (!ESTADO_SUCIA.equals(habitacion.getEstadoRack())) {
            throw new IllegalStateException("La habitación " + habitacion.getNumHabitacion()
                    + " no está pendiente de limpieza (estado actual: " + habitacion.getEstadoRack() + ")");
        }

        OrdenLimpieza orden = OrdenLimpieza.builder()
                .habitacion(habitacion)
                .empleado(empleado)
                .estadoLimpieza(ORDEN_EN_LIMPIEZA)
                .fHInicioToque1(OffsetDateTime.now())
                .build();
        orden = ordenLimpiezaRepository.save(orden);

        habitacion.setEstadoRack(ESTADO_EN_LIMPIEZA);
        habitacionRepository.save(habitacion);

        return orden;
    }

    @Override
    @Transactional
    public OrdenLimpieza finalizarLimpieza(Integer idOrden, String observacionesCamarera) {
        OrdenLimpieza orden = ordenLimpiezaRepository.findById(idOrden)
                .orElseThrow(() -> new NoSuchElementException("Orden de limpieza no encontrada: " + idOrden));

        if (ORDEN_LIMPIA.equals(orden.getEstadoLimpieza())) {
            throw new IllegalStateException("La orden " + idOrden + " ya fue finalizada");
        }

        OffsetDateTime fin = OffsetDateTime.now();
        orden.setFHFinToque2(fin);
        orden.setEstadoLimpieza(ORDEN_LIMPIA);
        orden.setObservacionesCamarera(observacionesCamarera);
        long minutos = Duration.between(orden.getFHInicioToque1(), fin).toMinutes();
        orden.setDuracionTurnaroundMin((short) Math.max(minutos, 0));
        orden = ordenLimpiezaRepository.save(orden);

        Habitacion habitacion = orden.getHabitacion();
        habitacion.setEstadoRack(ESTADO_DISPONIBLE);
        habitacionRepository.save(habitacion);

        return orden;
    }

    @Override
    @Transactional
    public ReporteAveria registrarAveria(AveriaRequestDTO request) {
        Habitacion habitacion = habitacionRepository.findById(request.getIdHabitacion())
                .orElseThrow(() -> new NoSuchElementException("Habitación no encontrada: " + request.getIdHabitacion()));
        Empleado empleado = empleadoRepository.findById(request.getIdEmpleado())
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado: " + request.getIdEmpleado()));

        boolean bloquea = GRAVEDADES_BLOQUEANTES.contains(request.getGravedad().toUpperCase());

        ReporteAveria averia = ReporteAveria.builder()
                .habitacion(habitacion)
                .empleado(empleado)
                .tituloIncidencia(request.getTituloIncidencia())
                .descripcionDano(request.getDescripcionDano())
                .gravedad(request.getGravedad().toUpperCase())
                .bloqueaHabitacion(bloquea)
                .fHReporte(OffsetDateTime.now())
                .build();
        averia = reporteAveriaRepository.save(averia);

        if (bloquea) {
            habitacion.setEstadoRack(ESTADO_MANTENIMIENTO);
            habitacionRepository.save(habitacion);
        }

        return averia;
    }

    @Override
    public List<ReporteAveria> listarAveriasPorHabitacion(Integer idHabitacion) {
        return reporteAveriaRepository.findByHabitacionId(idHabitacion);
    }
}
