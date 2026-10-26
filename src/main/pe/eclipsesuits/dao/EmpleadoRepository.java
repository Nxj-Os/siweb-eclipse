package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.Empleado;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByUsernameLogin(String usernameLogin);

    List<Empleado> findByEstadoActivoTrue();

    List<Empleado> findByRolEmpleadoId(Integer idRol);
}
