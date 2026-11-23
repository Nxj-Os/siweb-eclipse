package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.TransaccionPago;

import java.time.LocalDateTime;
import java.util.List;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    /**
     * Transacciones con fecha en [desde, hasta), con su comprobante cargado en la misma
     * consulta (evita el problema N+1 al leer el método de pago).
     */
    @Query("""
            select t from TransaccionPago t
            join fetch t.comprobantePago
            where t.fechaTransaccion >= :desde and t.fechaTransaccion < :hasta
            order by t.fechaTransaccion asc
            """)
    List<TransaccionPago> findByFechaTransaccionEntre(@Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta);
}
