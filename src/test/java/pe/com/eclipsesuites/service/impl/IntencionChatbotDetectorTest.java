package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntencionChatbotDetectorTest {

    private final IntencionChatbotDetector detector = new IntencionChatbotDetector();

    @Test
    void detectaDerivacionARecepcionAntesQueCualquierOtraIntencion() {
        assertEquals(Intencion.DERIVAR_RECEPCION,
                detector.detectar("Quiero hablar con recepción por mi reserva"));
        assertEquals(Intencion.DERIVAR_RECEPCION, detector.detectar("Necesito un humano"));
    }

    @Test
    void detectaTarifasConSinonimos() {
        assertEquals(Intencion.TARIFA, detector.detectar("¿Cuánto cuesta la suite?"));
        assertEquals(Intencion.TARIFA, detector.detectar("¿Cuáles son los precios?"));
        assertEquals(Intencion.TARIFA, detector.detectar("Necesito una cotización"));
    }

    @Test
    void detectaDisponibilidad() {
        assertEquals(Intencion.DISPONIBILIDAD, detector.detectar("¿Qué suites están libres?"));
        assertEquals(Intencion.DISPONIBILIDAD, detector.detectar("¿Hay plazas para mañana?"));
    }

    @Test
    void detectaServiciosUbicacionYPoliticas() {
        assertEquals(Intencion.SERVICIOS, detector.detectar("¿Incluye desayuno?"));
        assertEquals(Intencion.UBICACION, detector.detectar("¿Cuál es el horario de check-in?"));
        assertEquals(Intencion.POLITICAS, detector.detectar("¿Cuál es la política de cancelación?"));
    }

    @Test
    void detectaSaludoYDespedida() {
        assertEquals(Intencion.SALUDO, detector.detectar("Hola, buenos días"));
        assertEquals(Intencion.DESPEDIDA, detector.detectar("Gracias, adiós"));
    }

    @Test
    void ignoraAcentosYMayusculas() {
        assertEquals(Intencion.TARIFA, detector.detectar("CUANTO CUESTA"));
        assertEquals(Intencion.DERIVAR_RECEPCION, detector.detectar("HABLAR CON RECEPCIÓN"));
    }

    @Test
    void mensajeVacioOInvalidoQuedaComoOtro() {
        assertEquals(Intencion.OTRO, detector.detectar(null));
        assertEquals(Intencion.OTRO, detector.detectar("   "));
        assertEquals(Intencion.OTRO, detector.detectar("xyz sin palabras clave"));
    }

    @Test
    void detectaReservaAntesDeDisponibilidad() {
        assertEquals(Intencion.RESERVA, detector.detectar("Quiero reservar una suite"));
    }
}
