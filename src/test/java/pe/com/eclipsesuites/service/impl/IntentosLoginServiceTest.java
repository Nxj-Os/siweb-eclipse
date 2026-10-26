package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentosLoginServiceTest {

    @Test
    void verificarPermitido_bloqueaQuintoFalloPorIpEIdentidad() {
        IntentosLoginService servicio = new IntentosLoginService();

        for (int intento = 0; intento < 5; intento++) {
            servicio.registrarFallo("192.0.2.10", "ana@gmail.com");
        }

        LoginTemporalmenteBloqueadoException error = assertThrows(
                LoginTemporalmenteBloqueadoException.class,
                () -> servicio.verificarPermitido("192.0.2.10", "ANA@gmail.com"));
        assertTrue(error.getSegundosRestantes() > 0);
        assertTrue(error.getSegundosRestantes() <= 3 * 60);
    }

    @Test
    void verificarPermitido_aislaBloqueoPorIp() {
        IntentosLoginService servicio = new IntentosLoginService();
        for (int intento = 0; intento < 5; intento++) {
            servicio.registrarFallo("192.0.2.10", "admin");
        }

        servicio.verificarPermitido("192.0.2.11", "admin");
    }

    @Test
    void registrarExito_limpiaContador() {
        IntentosLoginService servicio = new IntentosLoginService();
        for (int intento = 0; intento < 4; intento++) {
            servicio.registrarFallo("192.0.2.10", "admin");
        }

        servicio.registrarExito("192.0.2.10", "admin");
        servicio.verificarPermitido("192.0.2.10", "admin");
    }
}
