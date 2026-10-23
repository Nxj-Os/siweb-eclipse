package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.Habitacion;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    Optional<Habitacion> findByNumHabitacion(String numHabitacion);

    List<Habitacion> findByEstadoRack(String estadoRack);

    List<Habitacion> findByTipoHabitacionId(Integer idTipoHabitacion);
}
