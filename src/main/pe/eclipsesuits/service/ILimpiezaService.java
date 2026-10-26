package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.request.AveriaRequestDTO;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.OrdenLimpieza;
import pe.com.eclipsesuites.entity.ReporteAveria;

import java.util.List;

public interface ILimpiezaService {

    List<Habitacion> listarHabitacionesPendientes();

    OrdenLimpieza iniciarLimpieza(Integer idHabitacion, Integer idEmpleado);

    OrdenLimpieza finalizarLimpieza(Integer idOrden, String observacionesCamarera);

    ReporteAveria registrarAveria(AveriaRequestDTO request);

    List<ReporteAveria> listarAveriasPorHabitacion(Integer idHabitacion);
}
