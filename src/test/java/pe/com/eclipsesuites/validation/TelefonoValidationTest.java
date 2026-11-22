package pe.com.eclipsesuites.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelefonoValidationTest {

    @Test
    void aceptaMovilPeruanoDeNueveDigitos() {
        assertTrue(TelefonoValidation.esValidoParaPais("+51985576482", "PE"));
        assertTrue(TelefonoValidation.esValidoParaPais("+51999123456", "PE"));
        assertTrue(TelefonoValidation.esValidoParaPais("+51999888777", "PE"));
    }

    @Test
    void rechazaFijoPeruanoDeOchoDigitos() {
        assertFalse(TelefonoValidation.esValidoParaPais("+5111234567", "PE"));
    }

    @Test
    void rechazaMovilPeruanoConMenosDeNueveDigitos() {
        assertFalse(TelefonoValidation.esValidoParaPais("+5199988877", "PE"));
    }

    @Test
    void rechazaNumeroDeOtroPaisCuandoSeEsperaPeru() {
        assertFalse(TelefonoValidation.esValidoParaPais("+14165550123", "PE"));
    }

    @Test
    void rechazaNumeroSinPrefijoInternacional() {
        assertFalse(TelefonoValidation.esValidoParaPais("985576482", "PE"));
    }

    @Test
    void rechazaEspacioEnLugarDeSignoMas() {
        assertFalse(TelefonoValidation.esValidoParaPais(" 51985576482", "PE"));
    }

    @Test
    void rechazaDobleCodigoDePais() {
        assertFalse(TelefonoValidation.esValidoParaPais("+5151985576482", "PE"));
    }

    @Test
    void aceptaNumeroValidoDeEstadosUnidos() {
        assertTrue(TelefonoValidation.esValidoParaPais("+12015550123", "US"));
    }

    @Test
    void rechazaNumeroEstadounidenseConLongitudIncorrecta() {
        assertFalse(TelefonoValidation.esValidoParaPais("+1201555012", "US"));
    }

    @Test
    void aceptaNumeroValidoDeEspana() {
        assertTrue(TelefonoValidation.esValidoParaPais("+34612345678", "ES"));
    }

    @Test
    void aceptaNumeroValidoDeBrasil() {
        assertTrue(TelefonoValidation.esValidoParaPais("+5511961234567", "BR"));
    }

    @Test
    void rechazaRegionNoSoportada() {
        assertFalse(TelefonoValidation.esValidoParaPais("+51985576482", "XX"));
    }

    @Test
    void rechazaValoresEnBlanco() {
        assertFalse(TelefonoValidation.esValidoParaPais(null, "PE"));
        assertFalse(TelefonoValidation.esValidoParaPais("+51985576482", null));
        assertFalse(TelefonoValidation.esValidoParaPais("", "PE"));
        assertFalse(TelefonoValidation.esValidoParaPais("+51985576482", ""));
    }
}
