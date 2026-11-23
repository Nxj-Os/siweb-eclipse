package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.eclipsesuites.entity.Habitacion;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    Optional<Habitacion> findByNumHabitacion(String numHabitacion);

    List<Habitacion> findByEstadoRack(String estadoRack);

    List<Habitacion> findByTipoHabitacionId(Integer idTipoHabitacion);

    /** Todas las habitaciones con su tipo cargado en una sola consulta, ordenadas por piso y número. */
    @Query("""
            select h from Habitacion h
            join fetch h.tipoHabitacion
            order by h.piso asc, h.numHabitacion asc
            """)
    List<Habitacion> findAllConTipo();
}
