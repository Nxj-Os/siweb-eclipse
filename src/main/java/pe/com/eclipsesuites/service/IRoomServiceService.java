package pe.com.eclipsesuites.service;

import pe.com.eclipsesuites.dto.request.CrearPedidoRequestDTO;
import pe.com.eclipsesuites.dto.response.CartaResponseDTO;
import pe.com.eclipsesuites.dto.response.PedidoResponseDTO;

/**
 * Carta de room service para la suite: la carta y la comanda no exigen
 * check-in (el huésped elige la suite de entrega; el QR se integrará
 * después), con precio congelado y seguimiento de estado.
 */
public interface IRoomServiceService {

    /**
     * Carta disponible (categorías, productos con precio/stock y
     * suites ocupadas para el selector de entrega).
     */
    CartaResponseDTO obtenerCarta(String token, String busqueda, Integer idCategoria);

    /**
     * Registra la comanda: congela precios, descuenta stock y genera
     * el código de pedido que verá el huésped.
     *
     * @throws pe.com.eclipsesuites.service.impl.StockInsuficienteException
     *         si algún producto no alcanza.
     */
    PedidoResponseDTO crearPedido(CrearPedidoRequestDTO request);

    /**
     * Estado actual de una comanda del huésped (con avance automático
     * por tiempo). Con token de check-in solo la suite dueña puede
     * leerla; sin token se consulta por código.
     */
    PedidoResponseDTO obtenerPedido(String token, String codigoPedido);

    /**
     * Fuerza el estado de una comanda desde el sistema de empleado
     * autenticado. Solo permite avances, nunca retrocesos.
     */
    PedidoResponseDTO actualizarEstadoPedido(String codigoPedido, String estado);
}
