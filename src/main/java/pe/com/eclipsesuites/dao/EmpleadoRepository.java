package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.Empleado;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    @EntityGraph(attributePaths = "rol")
    Optional<Empleado> findByUsernameLogin(String usernameLogin);

    boolean existsByUsernameLogin(String usernameLogin);
}
