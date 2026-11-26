package pe.com.eclipsesuites.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.ModalidadTurnoRepository;
import pe.com.eclipsesuites.dao.TarifaHabitacionRepository;
import pe.com.eclipsesuites.dao.TipoHabitacionRepository;
import pe.com.eclipsesuites.entity.Habitacion;
import pe.com.eclipsesuites.entity.ModalidadTurno;
import pe.com.eclipsesuites.entity.TarifaHabitacion;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;
import pe.com.eclipsesuites.entity.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Suites, tarifas y habitaciones de demostración para que el catálogo del
 * asistente tenga datos reales que consultar. Solo actúa con los perfiles
 * {@code demo} y {@code dev}, y únicamente si la base está vacía.
 */
@Slf4j
@Configuration
@Profile({"demo", "dev"})
@RequiredArgsConstructor
public class ChatbotDemoDataInitializer {

    private static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    private static final String ESTADO_OCUPADA = "OCUPADA";

    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final ModalidadTurnoRepository modalidadTurnoRepository;
    private final TarifaHabitacionRepository tarifaHabitacionRepository;
    private final HabitacionRepository habitacionRepository;

    @Bean
    public CommandLineRunner sembrarSuites() {
        return args -> {
            if (habitacionRepository.count() > 0) {
                return;
            }
            TipoHabitacion vip = crearTipo("VIP-DELUXE", "Suite VIP Deluxe",
                    "Elegancia moderna con detalles exclusivos para una experiencia premium.",
                    "/img/suite-vip-deluxe.webp", (short) 4);
            TipoHabitacion romana = crearTipo("ROMANA", "Suite Romana",
                    "Ambiente clásico con columnas, mármol y un toque de historia.",
                    "/img/suite-romana.webp", (short) 3);
            TipoHabitacion galaxia = crearTipo("GALAXIA", "Suite Galaxia",
                    "Una experiencia futurista con cielos estelares y luces LED.",
                    "/img/suite-galaxia.webp", (short) 4);

            ModalidadTurno diurno = crearModalidad("Diurno", new BigDecimal("8"),
                    LocalTime.of(8, 0), LocalTime.of(16, 0), false);
            ModalidadTurno nocturno = crearModalidad("Nocturno", new BigDecimal("16"),
                    LocalTime.of(16, 0), LocalTime.of(12, 0), true);

            crearTarifa(vip, diurno, "80.00", "15.00");
            crearTarifa(vip, nocturno, "140.00", "20.00");
            crearTarifa(romana, diurno, "75.00", "12.00");
            crearTarifa(romana, nocturno, "130.00", "18.00");
            crearTarifa(galaxia, diurno, "85.00", "15.00");
            crearTarifa(galaxia, nocturno, "150.00", "22.00");

            crearHabitacion("302", (short) 3, vip, ESTADO_DISPONIBLE);
            crearHabitacion("315", (short) 3, vip, ESTADO_DISPONIBLE);
            crearHabitacion("318", (short) 3, romana, ESTADO_OCUPADA);
            crearHabitacion("402", (short) 4, romana, ESTADO_OCUPADA);
            crearHabitacion("502", (short) 5, galaxia, ESTADO_OCUPADA);
            crearHabitacion("509", (short) 5, galaxia, ESTADO_DISPONIBLE);
            crearHabitacion("510", (short) 5, vip, ESTADO_DISPONIBLE);
            crearHabitacion("512", (short) 5, romana, ESTADO_OCUPADA);

            log.info("Se sembraron 3 suites, 2 modalidades, 6 tarifas y 8 habitaciones");
        };
    }

    private TipoHabitacion crearTipo(String codTipo, String nombre, String descripcion,
                                     String galeria, Short capacidad) {
        return tipoHabitacionRepository.save(TipoHabitacion.builder()
                .codTipo(codTipo)
                .nombreTematico(nombre)
                .descripcionTematica(descripcion)
                .galeriaFotos(galeria)
                .capacidadMaxima(capacidad)
                .amenidades("Wi-Fi|Smart TV|Agua caliente|Frigobar")
                .build());
    }

    private ModalidadTurno crearModalidad(String nombre, BigDecimal horas, LocalTime inicio,
                                          LocalTime fin, boolean amanecida) {
        return modalidadTurnoRepository.save(ModalidadTurno.builder()
                .nombreTurno(nombre)
                .duracionHoras(horas)
                .horaInicioFija(inicio)
                .horaFinFija(fin)
                .permiteAmanecida(amanecida)
                .build());
    }

    private void crearTarifa(TipoHabitacion tipo, ModalidadTurno modalidad, String precio,
                             String horaAdicional) {
        tarifaHabitacionRepository.save(TarifaHabitacion.builder()
                .id(new TarifaHabitacionPK(tipo.getId(), modalidad.getId()))
                .tipoHabitacion(tipo)
                .modalidadTurno(modalidad)
                .precioEstadiaBase(new BigDecimal(precio))
                .costoHoraAdicional(new BigDecimal(horaAdicional))
                .esTarifaFinSemana(false)
                .vigenciaDesde(LocalDate.now().minusDays(1))
                .vigenciaHasta(LocalDate.now().plusYears(1))
                .build());
    }

    private void crearHabitacion(String numero, Short piso, TipoHabitacion tipo, String estado) {
        habitacionRepository.save(Habitacion.builder()
                .tipoHabitacion(tipo)
                .numHabitacion(numero)
                .piso(piso)
                .estadoRack(estado)
                .tokenQrDinamico(UUID.randomUUID().toString())
                .fechaExpiracionToken(OffsetDateTime.now().plusYears(1))
                .bloqueoTemporalHasta(OffsetDateTime.now())
                .build());
    }
}
