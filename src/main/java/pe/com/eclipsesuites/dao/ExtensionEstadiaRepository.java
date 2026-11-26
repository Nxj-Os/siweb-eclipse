package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.EstadoExtensionReserva;
import pe.com.eclipsesuites.entity.ExtensionEstadia;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface ExtensionEstadiaRepository extends JpaRepository<ExtensionEstadia, Long> {

    @Query("""
            select count(e) > 0 from ExtensionEstadia e
            where e.reserva.habitacion.id = :habitacionId
              and e.fechaInicio < :fin and e.fechaFin > :inicio
              and e.estado = :estadoPendiente
              and e.reservaTemporalHasta > :ahora
            """)
    boolean existeCrucePendiente(@Param("habitacionId") Integer habitacionId,
                                @Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin,
                                @Param("estadoPendiente") EstadoExtensionReserva estadoPendiente,
                                @Param("ahora") OffsetDateTime ahora);

    @Query("""
            select count(e) > 0 from ExtensionEstadia e
            where e.id <> :extensionId
              and e.reserva.habitacion.id = :habitacionId
              and e.fechaInicio < :fin and e.fechaFin > :inicio
              and e.estado = :estadoPendiente
              and e.reservaTemporalHasta > :ahora
            """)
    boolean existeOtroCrucePendiente(@Param("extensionId") Long extensionId,
                                     @Param("habitacionId") Integer habitacionId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fin") LocalDateTime fin,
                                     @Param("estadoPendiente") EstadoExtensionReserva estadoPendiente,
                                     @Param("ahora") OffsetDateTime ahora);

    Optional<ExtensionEstadia> findByIdAndReservaClienteCorreoElectronicoIgnoreCase(
            Long id, String correoElectronico);

    @Modifying
    @Query("""
            update ExtensionEstadia e
            set e.estado = :estadoExpirado
            where e.estado = :estadoPendiente
              and e.reservaTemporalHasta <= :ahora
            """)
    int expirarExtensionesPendientes(
            @Param("estadoPendiente") EstadoExtensionReserva estadoPendiente,
            @Param("estadoExpirado") EstadoExtensionReserva estadoExpirado,
            @Param("ahora") OffsetDateTime ahora);
}
