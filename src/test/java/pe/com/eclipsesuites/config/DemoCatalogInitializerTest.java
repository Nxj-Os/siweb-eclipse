package pe.com.eclipsesuites.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.com.eclipsesuites.dao.HabitacionRepository;
import pe.com.eclipsesuites.dao.ModalidadTurnoRepository;
import pe.com.eclipsesuites.dao.TarifaHabitacionRepository;
import pe.com.eclipsesuites.dao.TipoHabitacionRepository;
import pe.com.eclipsesuites.entity.TarifaHabitacionPK;
import pe.com.eclipsesuites.entity.TipoHabitacion;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoCatalogInitializerTest {

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ModalidadTurnoRepository modalidadTurnoRepository;

    @Autowired
    private TarifaHabitacionRepository tarifaHabitacionRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Test
    void demoProfileSeedsSuitesRatesAndRooms() {
        var suites = tipoHabitacionRepository.findAll();

        assertThat(suites).hasSize(7);
        assertThat(suites).extracting(TipoHabitacion::getNombreTematico)
                .containsExactlyInAnyOrder(
                        "Suite Básica",
                        "Suite Junior",
                        "Suite Panorámica",
                        "Suite Roja",
                        "Suite Jacuzzi",
                        "Sauna con Jacuzzi",
                        "Suite Presidencial");
        assertThat(modalidadTurnoRepository.findAll()).hasSize(3);
        assertThat(tarifaHabitacionRepository.findAll()).hasSize(21);
        assertThat(habitacionRepository.findAll()).hasSize(16);
        assertThat(suites).allSatisfy(suite ->
                assertThat(habitacionRepository.countByTipoHabitacionId(suite.getId())).isPositive());
    }

    @Test
    void demoProfileSeedsAmenitiesAndOfficialPrices() {
        var suites = tipoHabitacionRepository.findAll();
        assertThat(suites).allSatisfy(suite ->
                assertThat(suite.getAmenidades()).isNotBlank());

        assertThat(precioDe("BASICA", "4 horas")).isEqualByComparingTo("50.00");
        assertThat(precioDe("JUNIOR", "6 horas")).isEqualByComparingTo("90.00");
        assertThat(precioDe("PANORAMICA", "12 horas")).isEqualByComparingTo("170.00");
        assertThat(precioDe("ROJA", "4 horas")).isEqualByComparingTo("100.00");
        assertThat(precioDe("JACUZZI", "6 horas")).isEqualByComparingTo("160.00");
        assertThat(precioDe("SAUNA_JACUZZI", "12 horas")).isEqualByComparingTo("250.00");
        assertThat(precioDe("PRESIDENCIAL", "12 horas")).isEqualByComparingTo("260.00");
    }

    private BigDecimal precioDe(String codTipo, String nombreModalidad) {
        Integer tipoId = tipoHabitacionRepository.findByCodTipo(codTipo)
                .orElseThrow()
                .getId();
        Integer modalidadId = modalidadTurnoRepository.findByNombreTurno(nombreModalidad)
                .orElseThrow()
                .getId();
        return tarifaHabitacionRepository
                .findById(new TarifaHabitacionPK(tipoId, modalidadId))
                .orElseThrow()
                .getPrecioEstadiaBase();
    }
}
