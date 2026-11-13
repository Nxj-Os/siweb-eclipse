package pe.com.eclipsesuites.dao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.Habitacion;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    Optional<Habitacion> findByNumHabitacion(String numHabitacion);

    List<Habitacion> findByEstadoRack(String estadoRack);

    List<Habitacion> findByTipoHabitacionId(Integer idTipoHabitacion);

    long countByTipoHabitacionId(Integer idTipoHabitacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.tipoHabitacion.id = :tipoId order by h.id")
    List<Habitacion> findAllByTipoHabitacionIdForUpdate(@Param("tipoId") Integer tipoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.id = :habitacionId")
    Optional<Habitacion> findByIdForUpdate(@Param("habitacionId") Integer habitacionId);
}
