package pe.com.eclipsesuites.service.gemini;

public record GeminiTurno(String rol, String texto) {

    public static GeminiTurno usuario(String texto) {
        return new GeminiTurno("user", texto);
    }

    public static GeminiTurno modelo(String texto) {
        return new GeminiTurno("model", texto);
    }
}
