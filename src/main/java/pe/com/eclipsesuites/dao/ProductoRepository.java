package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * Carta completa con la categoría resuelta (JOIN FETCH) para
     * evitar inicializaciones perezosas fuera de la transacción.
     */
    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria ORDER BY p.categoria.nombreCategoria, p.nombre")
    List<Producto> findAllConCategoria();

    /**
     * Descuento atómico de existencias: solo descuenta si queda stock
     * suficiente. Devuelve el número de filas afectadas (0 = sin stock).
     */
    @Modifying
    @Query("UPDATE Producto p SET p.stockDisponible = p.stockDisponible - :cantidad "
            + "WHERE p.id = :idProducto AND p.stockDisponible >= :cantidad")
    int descontarStock(@Param("idProducto") Integer idProducto, @Param("cantidad") Integer cantidad);
}
