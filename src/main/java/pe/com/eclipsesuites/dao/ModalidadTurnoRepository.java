package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.ModalidadTurno;

import java.util.Optional;

public interface ModalidadTurnoRepository extends JpaRepository<ModalidadTurno, Integer> {

    Optional<ModalidadTurno> findByNombreTurno(String nombreTurno);
}
