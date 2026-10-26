package pe.com.eclipsesuites.service.impl;

public class DatosRegistroNoDisponiblesException extends RuntimeException {

    public DatosRegistroNoDisponiblesException() {
        super("No se pudo completar el registro; verifica la información ingresada");
    }

    public DatosRegistroNoDisponiblesException(String message) {
        super(message);
    }
}
