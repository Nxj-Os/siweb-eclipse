package pe.com.eclipsesuites.service.impl;

/**
 * Intención detectada en el mensaje del usuario. Se persiste en
 * {@code mensaje_chatbot.intencion_detectada}.
 */
public enum Intencion {

    SALUDO("Saludo"),
    TARIFA("Tarifas y precios"),
    DISPONIBILIDAD("Disponibilidad de suites"),
    SERVICIOS("Servicios del hotel"),
    UBICACION("Ubicación y contacto"),
    POLITICAS("Políticas de estadía"),
    RESERVA("Solicitud de reserva"),
    DERIVAR_RECEPCION("Derivación a recepción"),
    DESPEDIDA("Despedida"),
    OTRO("Consulta general");

    private final String etiqueta;

    Intencion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
