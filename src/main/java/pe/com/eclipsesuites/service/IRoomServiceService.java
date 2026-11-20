package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.request.CrearPedidoRequestDTO;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;

/**
 * Carta de room service para la suite: acceso por token de check-in,
 * creación de comandas con precio congelado y seguimiento de estado.
 */
public interface IRoomServiceService {

    /**
     * Carta disponible (categorías y productos con precio/stock) para
     * la habitación identificada por el token de check-in.
     *
     * @throws pe.com.eclipsesuites.service.impl.AccesoRoomServiceNoPermitidoException
     *         si el token no existe o expiró.
     */
    CartaResponseDTO obtenerCarta(String token, String busqueda, Integer idCategoria);

    /**
     * Registra la comanda: congela precios, descuenta stock y genera
     * el código de pedido que verá el huésped.
     *
     * @throws pe.com.eclipsesuites.service.impl.AccesoRoomServiceNoPermitidoException
     *         si el token de check-in no es válido.
     * @throws pe.com.eclipsesuites.service.impl.StockInsuficienteException
     *         si algún producto no alcanza.
     */
    PedidoResponseDTO crearPedido(CrearPedidoRequestDTO request);

    /**
     * Estado actual de una comanda del huésped (con avance automático
     * por tiempo). Solo la habitación dueña del token puede leerla.
     */
    PedidoResponseDTO obtenerPedido(String token, String codigoPedido);

    /**
     * Fuerza el estado de una comanda desde el sistema de empleado
     * autenticado. Solo permite avances, nunca retrocesos.
     */
    PedidoResponseDTO actualizarEstadoPedido(String codigoPedido, String estado);
}
