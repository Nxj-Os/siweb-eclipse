package pe.com.eclipsesuites.service.gemini;

public class GeminiNoDisponibleException extends RuntimeException {

    public GeminiNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public GeminiNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
