package pe.com.eclipsesuites.service.impl;

public class DatosRegistroNoDisponiblesException extends IllegalArgumentException {

    public DatosRegistroNoDisponiblesException() {
        super("No se pudo completar el registro; verifica la información ingresada");
    }
}
