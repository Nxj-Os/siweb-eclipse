package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.OrdenLimpieza;

import java.util.List;
import java.util.Optional;

public interface OrdenLimpiezaRepository extends JpaRepository<OrdenLimpieza, Integer> {

    List<OrdenLimpieza> findByEstadoLimpieza(String estadoLimpieza);

    List<OrdenLimpieza> findByHabitacionId(Integer idHabitacion);

    Optional<OrdenLimpieza> findTopByHabitacionIdOrderByIdDesc(Integer idHabitacion);
}
