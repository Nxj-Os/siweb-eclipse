package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.ReporteAveria;

import java.util.List;

public interface ReporteAveriaRepository extends JpaRepository<ReporteAveria, Integer> {

    List<ReporteAveria> findByHabitacionId(Integer idHabitacion);

    List<ReporteAveria> findByBloqueaHabitacionTrue();

    List<ReporteAveria> findByGravedad(String gravedad);
}
