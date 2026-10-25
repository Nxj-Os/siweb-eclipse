package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.TipoHabitacion;

import java.util.Optional;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Integer> {

    Optional<TipoHabitacion> findByCodTipo(String codTipo);
}
