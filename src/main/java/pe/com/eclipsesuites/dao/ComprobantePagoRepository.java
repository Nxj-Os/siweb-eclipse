package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.ComprobantePago;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Long> {
}
