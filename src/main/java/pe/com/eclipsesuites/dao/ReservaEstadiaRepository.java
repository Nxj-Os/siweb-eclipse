package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.EstadoReserva;
import pe.com.eclipsesuites.entity.ReservaEstadia;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaEstadiaRepository extends JpaRepository<ReservaEstadia, Long> {

    List<ReservaEstadia> findByClienteCorreoElectronicoIgnoreCaseOrderByFechaCreacionDesc(
            String correoElectronico);

    Optional<ReservaEstadia> findByIdAndClienteCorreoElectronicoIgnoreCase(
            Long id, String correoElectronico);

    Optional<ReservaEstadia> findByTokenQr(String tokenQr);

    List<ReservaEstadia> findByEstadoReservaOrderByFechaCreacionAsc(EstadoReserva estadoReserva);

    long countByTipoHabitacionIdAndEstadoReservaAndFechaInicioBeforeAndFechaFinAfter(
            Integer idTipoHabitacion, EstadoReserva estadoReserva,
            LocalDateTime fechaFin, LocalDateTime fechaInicio);

    @Query("""
            select count(r) > 0 from ReservaEstadia r
            where r.habitacion.id = :habitacionId
              and r.fechaInicio < :fin and r.fechaFin > :inicio
              and (r.estadoReserva in :estadosActivos
                   or (r.estadoReserva = :estadoPendiente
                       and r.reservaTemporalHasta > :ahora))
            """)
    boolean existeCruceActivo(@Param("habitacionId") Integer habitacionId,
                              @Param("inicio") LocalDateTime inicio,
                              @Param("fin") LocalDateTime fin,
                              @Param("estadosActivos") List<EstadoReserva> estadosActivos,
                              @Param("estadoPendiente") EstadoReserva estadoPendiente,
                              @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("""
            update ReservaEstadia r
            set r.estadoReserva = :estadoExpirado
            where r.estadoReserva = :estadoPendiente
              and r.reservaTemporalHasta <= :ahora
            """)
    int expirarReservasPendientes(@Param("estadoPendiente") EstadoReserva estadoPendiente,
                                  @Param("estadoExpirado") EstadoReserva estadoExpirado,
                                  @Param("ahora") OffsetDateTime ahora);
}