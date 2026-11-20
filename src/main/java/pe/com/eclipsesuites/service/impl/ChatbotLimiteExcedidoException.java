package pe.com.eclipsesuites.service.impl;

public class ChatbotLimiteExcedidoException extends RuntimeException {

    public ChatbotLimiteExcedidoException(String mensaje) {
        super(mensaje);
    }
}
