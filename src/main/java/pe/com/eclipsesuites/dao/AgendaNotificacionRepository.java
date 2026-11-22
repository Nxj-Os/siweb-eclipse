package pe.com.eclipsesuites.dao;
import pe.com.eclipsesuites.entity.AgendaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AgendaNotificacionRepository extends JpaRepository<AgendaNotificacion,Long> {
 List<AgendaNotificacion> findTop100ByDestinatarioInOrderByFechaDesc(List<String> destinatarios);
}
