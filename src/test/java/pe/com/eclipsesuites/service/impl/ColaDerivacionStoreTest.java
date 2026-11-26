package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColaDerivacionStoreTest {

    @Test
    void unaSesionEntraYSaleDeLaCola() {
        ColaDerivacionStore store = new ColaDerivacionStore();

        assertFalse(store.enCola("sesion-1"));

        store.entrar("sesion-1");

        assertTrue(store.enCola("sesion-1"));
        assertFalse(store.enCola("sesion-2"));
        assertEquals(ColaDerivacionStore.TIEMPO_ESTIMADO_MINUTOS,
                store.minutosRestantes("sesion-1"));
        assertFalse(store.superoEstimacion("sesion-1"));

        store.salir("sesion-1");

        assertFalse(store.enCola("sesion-1"));
        assertEquals(0, store.minutosRestantes("sesion-1"));
    }

    @Test
    void idSesionNuloONuloNoGeneraEstado() {
        ColaDerivacionStore store = new ColaDerivacionStore();

        store.entrar(null);
        store.entrar("   ");

        assertFalse(store.enCola(null));
        assertFalse(store.enCola("   "));
        assertEquals(0, store.minutosRestantes(null));
    }
}
