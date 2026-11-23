package pe.com.eclipsesuites.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Estructura de las entidades de la carta de room service, con foco en
 * la clave primaria compuesta (id_pedido, id_producto) que exige el
 * modelo físico del informe.
 */
class RoomServiceEntityStructureTest {

    @Test
    void categoriaProductoShouldBeJpaEntity() {
        assertTrue(CategoriaProducto.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void productoShouldBeJpaEntity() {
        assertTrue(Producto.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void pedidoRoomServiceShouldBeJpaEntity() {
        assertTrue(PedidoRoomService.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void detallePedidoRSShouldUseCompositeKey() {
        assertTrue(DetallePedidoRS.class.isAnnotationPresent(Entity.class));
        IdClass idClass = DetallePedidoRS.class.getAnnotation(IdClass.class);
        assertEquals(DetallePedidoRSPK.class, idClass.value());

        long camposId = Arrays.stream(DetallePedidoRS.class.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(Id.class))
                .count();
        assertEquals(2, camposId);
    }

    @Test
    void detallePedidoRSPKShouldExposePedidoYProducto() {
        assertTrue(Serializable.class.isAssignableFrom(DetallePedidoRSPK.class));
        DetallePedidoRSPK pk = new DetallePedidoRSPK(55, 10);
        assertEquals(55, pk.getIdPedido());
        assertEquals(10, pk.getIdProducto());
        assertEquals(pk, new DetallePedidoRSPK(55, 10));
    }

    @Test
    void estadosDelPedidoAvanzanEnElOrdenDelFlujo() {
        assertArrayEquals(
                new EstadoPedido[]{EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION,
                        EstadoPedido.CONTROLADO, EstadoPedido.ENTREGADO},
                EstadoPedido.values());
        assertTrue(EstadoPedido.RECIBIDO.esAnteriorA(EstadoPedido.ENTREGADO));
        assertTrue(!EstadoPedido.ENTREGADO.esAnteriorA(EstadoPedido.RECIBIDO));
    }
}
