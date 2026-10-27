package pe.com.eclipsesuites.service.impl;

public class LoginTemporalmenteBloqueadoException extends RuntimeException {

    private final long segundosRestantes;

    public LoginTemporalmenteBloqueadoException(long segundosRestantes) {
        super("Credenciales inválidas. Intenta nuevamente en " + segundosRestantes + " segundos.");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
