package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.eclipsesuites.entity.EstadoPedido;
import pe.com.eclipsesuites.entity.PedidoRoomService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRoomServiceRepository extends JpaRepository<PedidoRoomService, Integer> {

    Optional<PedidoRoomService> findByCodigoPedido(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    List<PedidoRoomService> findByHabitacionId(Integer idHabitacion);

    long countByEstadoPedidoIn(Collection<EstadoPedido> estados);

    /** Comandas del panel del empleado, con suite e ítems ya resueltos. */
    @Query("select distinct p from PedidoRoomService p"
            + " join fetch p.habitacion"
            + " left join fetch p.detalles"
            + " order by p.fHPedido desc")
    List<PedidoRoomService> findAllConDetalles();
}
