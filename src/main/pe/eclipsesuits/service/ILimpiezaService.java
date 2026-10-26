package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.request.AveriaRequestDTO;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.OrdenLimpieza;
import pe.com.eclipsesuites.entity.ReporteAveria;

import java.util.List;

/**
 * Módulo de Turnaround/Desinfección y Averías (RF-13 a RF-16).
 * Pensado para el flujo móvil de limpieza en máximo 2 toques (RNF-06):
 * 1er toque -> iniciarLimpieza, 2do toque -> finalizarLimpieza.
 */
public interface ILimpiezaService {

    /** RF-13: habitaciones desocupadas en estado SUCIA, pendientes de limpieza. */
    List<Habitacion> listarHabitacionesPendientes();

    /** RF-14 (toque 1): inicia el turnaround de una habitación. */
    OrdenLimpieza iniciarLimpieza(Integer idHabitacion, Integer idEmpleado);

    /** RF-14 (toque 2) + RF-15: cierra la orden y habilita la habitación como DISPONIBLE. */
    OrdenLimpieza finalizarLimpieza(Integer idOrden, String observacionesCamarera);

    /** RF-16: registra una avería técnica y, si corresponde, bloquea la habitación. */
    ReporteAveria registrarAveria(AveriaRequestDTO request);

    List<ReporteAveria> listarAveriasPorHabitacion(Integer idHabitacion);
}
