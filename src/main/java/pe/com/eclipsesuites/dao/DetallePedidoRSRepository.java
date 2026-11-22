package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.DetallePedidoRS;
import pe.com.eclipsesuites.entity.DetallePedidoRSPK;

import java.util.List;

public interface DetallePedidoRSRepository extends JpaRepository<DetallePedidoRS, DetallePedidoRSPK> {

    List<DetallePedidoRS> findByIdPedido(Integer idPedido);
}
