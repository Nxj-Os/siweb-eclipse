package pe.com.eclipsesuites.service.impl;

/**
 * No existe ninguna comanda con el código indicado (o pertenece a
 * otra habitación y no se revela su existencia).
 */
public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(String codigoPedido) {
        super("No se encontró el pedido " + codigoPedido);
    }
}
