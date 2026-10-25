package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;

import java.util.List;

public interface TarifaHabitacionRepository extends JpaRepository<TarifaHabitacion, TarifaHabitacionPK> {

    List<TarifaHabitacion> findByIdIdTipoHabitacion(Integer idTipoHabitacion);
}
