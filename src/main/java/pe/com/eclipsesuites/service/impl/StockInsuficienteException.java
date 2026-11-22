package pe.com.eclipsesuites.service.impl;

/**
 * Uno de los productos del pedido no tiene stock suficiente.
 * Lleva el detalle para mostrarlo al huésped y para que la carta
 * refresque la existencia real del producto.
 */
public class StockInsuficienteException extends RuntimeException {

    private final String nombreProducto;
    private final Integer stockDisponible;

    public StockInsuficienteException(String nombreProducto, Integer stockDisponible) {
        super("Stock insuficiente para " + nombreProducto
                + "; quedan " + stockDisponible);
        this.nombreProducto = nombreProducto;
        this.stockDisponible = stockDisponible;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }
}
