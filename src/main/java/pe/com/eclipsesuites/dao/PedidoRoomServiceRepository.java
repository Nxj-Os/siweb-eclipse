package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.PedidoRoomService;

import java.util.List;
import java.util.Optional;

public interface PedidoRoomServiceRepository extends JpaRepository<PedidoRoomService, Integer> {

    Optional<PedidoRoomService> findByCodigoPedido(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    List<PedidoRoomService> findByHabitacionId(Integer idHabitacion);
}
