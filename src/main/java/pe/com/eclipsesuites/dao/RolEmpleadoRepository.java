package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.RolEmpleado;

import java.util.Optional;

public interface RolEmpleadoRepository extends JpaRepository<RolEmpleado, Integer> {

    Optional<RolEmpleado> findByNombreRol(String nombreRol);
}
