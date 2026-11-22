package pe.com.eclipsesuites.dao;
import pe.com.eclipsesuites.entity.AgendaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AgendaEventoRepository extends JpaRepository<AgendaEvento,Long> {
 List<AgendaEvento> findAllByOrderByInicioAsc();
}
