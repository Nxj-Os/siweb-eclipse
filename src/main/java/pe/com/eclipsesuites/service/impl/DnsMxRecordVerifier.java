package pe.com.eclipsesuites.service.impl;

import org.springframework.stereotype.Component;
import pe.com.eclipsesuites.service.MxRecordVerifier;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Component
public class DnsMxRecordVerifier implements MxRecordVerifier {

    @Override
    public boolean tieneRegistroMx(String dominio) {
        Hashtable<String, String> entorno = new Hashtable<>();
        entorno.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        entorno.put("com.sun.jndi.dns.timeout.initial", "2000");
        entorno.put("com.sun.jndi.dns.timeout.retries", "1");

        DirContext contexto = null;
        try {
            contexto = new InitialDirContext(entorno);
            Attributes registros = contexto.getAttributes(dominio, new String[]{"MX"});
            return registros.get("MX") != null && registros.get("MX").size() > 0;
        } catch (NameNotFoundException ex) {
            return false;
        } catch (NamingException ex) {
            throw new IllegalStateException("No se pudo verificar el dominio del correo", ex);
        } finally {
            if (contexto != null) {
                try {
                    contexto.close();
                } catch (NamingException ex) {
                    throw new IllegalStateException("No se pudo cerrar la consulta DNS", ex);
                }
            }
        }
    }
}
