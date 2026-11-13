package pe.com.eclipsesuites.entity;

public enum EstadoReserva {
    PENDIENTE_PAGO("Pago pendiente"),
    CONFIRMADA("Confirmada"),
    CHECKED_IN("Check-in realizado"),
    CANCELADA("Cancelada"),
    EXPIRADA("Tiempo de pago vencido");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
