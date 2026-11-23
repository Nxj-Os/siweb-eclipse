package pe.com.eclipsesuites.entity;

public enum MetodoPagoReserva {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    YAPE("Yape"),
    PLIN("Plin"),
    PRESENCIAL("Pago en el local");

    private final String etiqueta;

    MetodoPagoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
