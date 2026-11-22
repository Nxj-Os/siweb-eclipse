package pe.com.eclipsesuites.entity;

/**
 * Ciclo de vida de una comanda de room service, según el flujo S2
 * del informe y los estados mostrados al huésped (Figma C-09):
 * RECIBIDO → EN_PREPARACION → CONTROLADO → ENTREGADO.
 * El orden de los valores define el avance hacia adelante.
 */
public enum EstadoPedido {

    RECIBIDO,
    EN_PREPARACION,
    CONTROLADO,
    ENTREGADO;

    public boolean esAnteriorA(EstadoPedido otro) {
        return this.ordinal() < otro.ordinal();
    }
}
