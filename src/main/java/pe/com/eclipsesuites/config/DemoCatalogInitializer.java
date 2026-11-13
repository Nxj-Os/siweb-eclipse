package pe.com.eclipsesuites.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoCatalogInitializer implements ApplicationRunner {

    private static final List<DemoSuite> SUITES = List.of(
            new DemoSuite("BASICA", "Suite Básica",
                    "Una opción clásica y confortable para una estadía breve.",
                    "/img/suite-romana.webp", (short) 2, List.of("201", "202"),
                    List.of(50, 80, 120)),
            new DemoSuite("JUNIOR", "Suite Junior",
                    "Un ambiente temático para descansar y relajarse.",
                    "/img/suite-vip-deluxe.webp", (short) 2, List.of("301", "303"),
                    List.of(80, 120, 170)),
            new DemoSuite("PANORAMICA", "Suite Panorámica",
                    "Disfruta de una vista panorámica al exterior. Incluye cama matrimonial, Smart TV, frigobar, agua caliente, luces psicodélicas, ventilador, espejo de pared, Wi-Fi y videos.",
                    "/img/suite-galaxia.webp", (short) 2,
                    List.of("302", "316", "318", "402", "502", "509", "510", "512"),
                    List.of(80, 120, 170)),
            new DemoSuite("ROJA", "Suite Roja",
                    "Una suite temática con ambientación en tonos rojos.",
                    "/img/suite-placeholder.svg", (short) 2, List.of("401"),
                    List.of(150, 200, 270)),
            new DemoSuite("JACUZZI", "Suite Jacuzzi",
                    "Una suite privada para disfrutar de un momento de descanso.",
                    "/img/suite-galaxia.webp", (short) 2, List.of("501"),
                    List.of(170, 230, 310)),
            new DemoSuite("SAUNA_JACUZZI", "Sauna con Jacuzzi",
                    "Un espacio privado de demostración con sauna y jacuzzi.",
                    "/img/suite-placeholder.svg", (short) 2, List.of("601"),
                    List.of(200, 260, 350)),
            new DemoSuite("PRESIDENCIAL", "Suite Presidencial",
                    "Una suite de categoría superior para una estadía especial.",
                    "/img/suite-vip-deluxe.webp", (short) 2, List.of("701"),
                    List.of(250, 320, 420))
    );

    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final ModalidadTurnoRepository modalidadTurnoRepository;
    private final TarifaHabitacionRepository tarifaHabitacionRepository;
    private final HabitacionRepository habitacionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ModalidadTurno> modalidades = List.of(
                obtenerOCrearModalidad("4 horas", "4.00", LocalTime.of(18, 0),
                        LocalTime.of(22, 0), false),
                obtenerOCrearModalidad("6 horas", "6.00", LocalTime.of(18, 0),
                        LocalTime.MIDNIGHT, true),
                obtenerOCrearModalidad("12 horas", "12.00", LocalTime.of(18, 0),
                        LocalTime.of(6, 0), true)
        );

        for (DemoSuite demoSuite : SUITES) {
            TipoHabitacion tipo = obtenerOCrearSuite(demoSuite);
            crearTarifasDemo(tipo, modalidades, demoSuite.precios());
            demoSuite.habitaciones().forEach(numero -> crearHabitacionDemo(tipo, numero));
        }

        log.info("Catálogo de demostración listo: {} suites, {} modalidades y precios de muestra.",
                SUITES.size(), modalidades.size());
    }

    private ModalidadTurno obtenerOCrearModalidad(String nombre, String duracion,
                                                   LocalTime inicio, LocalTime fin,
                                                   boolean permiteAmanecida) {
        return modalidadTurnoRepository.findByNombreTurno(nombre)
                .orElseGet(() -> modalidadTurnoRepository.save(ModalidadTurno.builder()
                        .nombreTurno(nombre)
                        .duracionHoras(new BigDecimal(duracion))
                        .horaInicioFija(inicio)
                        .horaFinFija(fin)
                        .permiteAmanecida(permiteAmanecida)
                        .build()));
    }

    private TipoHabitacion obtenerOCrearSuite(DemoSuite demoSuite) {
        return tipoHabitacionRepository.findByCodTipo(demoSuite.codigo())
                .orElseGet(() -> tipoHabitacionRepository.save(TipoHabitacion.builder()
                        .codTipo(demoSuite.codigo())
                        .nombreTematico(demoSuite.nombre())
                        .descripcionTematica(demoSuite.descripcion())
                        .galeriaFotos(demoSuite.imagen())
                        .capacidadMaxima(demoSuite.capacidad())
                        .build()));
    }

    private void crearTarifasDemo(TipoHabitacion tipo, List<ModalidadTurno> modalidades,
                                  List<Integer> precios) {
        LocalDate vigenciaDesde = LocalDate.now().minusDays(1);
        LocalDate vigenciaHasta = LocalDate.now().plusYears(5);
        for (int index = 0; index < modalidades.size(); index++) {
            ModalidadTurno modalidad = modalidades.get(index);
            TarifaHabitacionPK tarifaId = new TarifaHabitacionPK(tipo.getId(), modalidad.getId());
            if (tarifaHabitacionRepository.existsById(tarifaId)) {
                continue;
            }
            tarifaHabitacionRepository.save(TarifaHabitacion.builder()
                    .id(tarifaId)
                    .tipoHabitacion(tipo)
                    .modalidadTurno(modalidad)
                    .precioEstadiaBase(BigDecimal.valueOf(precios.get(index)))
                    .costoHoraAdicional(BigDecimal.ZERO)
                    .esTarifaFinSemana(false)
                    .vigenciaDesde(vigenciaDesde)
                    .vigenciaHasta(vigenciaHasta)
                    .build());
        }
    }

    private void crearHabitacionDemo(TipoHabitacion tipo, String numeroHabitacion) {
        if (habitacionRepository.findByNumHabitacion(numeroHabitacion).isPresent()) {
            return;
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        habitacionRepository.save(Habitacion.builder()
                .tipoHabitacion(tipo)
                .numHabitacion(numeroHabitacion)
                .piso((short) 1)
                .estadoRack("LIBRE")
                .tokenQrDinamico("demo-" + UUID.randomUUID())
                .fechaExpiracionToken(ahora.plusYears(1))
                .bloqueoTemporalHasta(ahora.minusDays(1))
                .build());
    }

    private record DemoSuite(String codigo, String nombre,
                             String descripcion, String imagen, short capacidad,
                             List<String> habitaciones,
                             List<Integer> precios) {
    }
}
