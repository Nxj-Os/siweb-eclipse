package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;
import pe.com.eclipsesuites.service.MxRecordVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorreoDomainPolicyTest {

    private final MxRecordVerifier mxRecordVerifier = mock(MxRecordVerifier.class);
    private final CorreoDomainPolicy policy = new CorreoDomainPolicy(
            "gmail.com,example.org",
            "mailinator.com",
            mxRecordVerifier);

    @Test
    void validar_normalizaYAceptaDominioPermitidoConMx() {
        when(mxRecordVerifier.tieneRegistroMx("gmail.com")).thenReturn(true);

        assertEquals("ana@gmail.com", policy.validar("ana@gmail.com", true));
    }

    @Test
    void validar_rechazaDominioNoPermitido() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validar("ana@dominioinventado.net", false));
    }

    @Test
    void validar_rechazaDominioDesechableAunqueTengaMx() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validar("ana@mailinator.com", false));
    }

    @Test
    void validar_rechazaDominioSinMx() {
        when(mxRecordVerifier.tieneRegistroMx("example.org")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> policy.validar("ana@example.org", true));
    }
}
