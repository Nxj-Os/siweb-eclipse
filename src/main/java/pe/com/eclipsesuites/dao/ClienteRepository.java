package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	Optional<Cliente> findByCorreoElectronicoIgnoreCase(String correoElectronico);

	boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);

	boolean existsByNumDocumento(String numDocumento);
}
