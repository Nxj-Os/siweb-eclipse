package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.TransaccionPago;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {
}
