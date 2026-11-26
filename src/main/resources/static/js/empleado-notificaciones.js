(() => {
    'use strict';

    // Tablero de novedades del portal de empleados: reservas web + comandas
    // de room service en el mismo panel, refrescado en vivo.
    const aside = document.querySelector('.notifications');
    if (!aside) return;

    // Le avisa al demo del rack que este bloque ya no es contenido de prueba.
    aside.dataset.realtime = '1';

    const ICONO = { RESERVA: '🆕', CHECK_IN: '🔑', COMANDA: '🍔' };
    const ETIQUETA_RESUMEN = {
        reservasNuevas: 'reserva(s) pendiente(s)',
        pendientesCheckIn: 'confirmada(s) por check-in',
        reservasEnCurso: 'estadía(s) en curso',
        comandasActivas: 'comanda(s) activa(s)'
    };

    const esc = (v) => String(v == null ? '' : v)
        .replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    function parseFecha(valor) {
        if (!valor) return null;
        if (Array.isArray(valor)) {
            return new Date(valor[0], (valor[1] || 1) - 1, valor[2] || 1,
                valor[3] || 0, valor[4] || 0, valor[5] || 0);
        }
        const fecha = new Date(valor);
        return isNaN(fecha.getTime()) ? null : fecha;
    }

    function relativa(valor) {
        const fecha = parseFecha(valor);
        if (!fecha) return '—';
        const minutos = Math.round((Date.now() - fecha.getTime()) / 60000);
        if (minutos < 1) return 'Ahora';
        if (minutos < 60) return 'Hace ' + minutos + ' min';
        const horas = Math.round(minutos / 60);
        if (horas < 24) return 'Hace ' + horas + ' h';
        return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit' });
    }

    function resumenTexto(resumen) {
        if (!resumen) return 'Reservas y room service en vivo';
        return Object.keys(resumen)
            .map((clave) => (resumen[clave] > 0 ? resumen[clave] + ' ' + (ETIQUETA_RESUMEN[clave] || clave) : null))
            .filter(Boolean)
            .join(' · ') || 'Sin novedades por ahora';
    }

    function pintar(datos) {
        const avisos = Array.isArray(datos.notificaciones) ? datos.notificaciones : [];
        const cuerpo = avisos.length
            ? avisos.map((aviso) => {
                const icono = ICONO[aviso.tipo] || '🔔';
                return '<div class="notice">'
                    + '<b>' + icono + ' ' + esc(aviso.titulo) + '</b>'
                    + '<span>' + esc(aviso.detalle) + '</span>'
                    + '<small>' + esc(relativa(aviso.fecha)) + '</small>'
                    + '</div>';
            }).join('')
            : '<div class="notice"><b>📭 Sin novedades</b>'
                + '<span>Todavía no hay reservas ni comandas registradas</span>'
                + '<small>Ahora</small></div>';

        aside.innerHTML = '<h2>Notificaciones</h2><p>' + esc(resumenTexto(datos.resumen))
            + '</p>' + cuerpo;
    }

    function pintarError(mensaje) {
        aside.innerHTML = '<h2>Notificaciones</h2><p>No se pudieron cargar las novedades</p>'
            + '<div class="notice"><b>⚠️ ' + esc(mensaje) + '</b>'
            + '<span>Revisa tu sesión de empleado</span><small>Ahora</small></div>';
    }

    async function cargar() {
        try {
            const respuesta = await fetch('/api/empleado/novedades', {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            });
            if (!respuesta.ok) {
                pintarError(respuesta.status === 401 ? 'Sesión expirada'
                    : respuesta.status === 403 ? 'Sin permisos para este panel'
                        : 'Error ' + respuesta.status);
                return;
            }
            pintar(await respuesta.json());
        } catch (error) {
            pintarError(error && error.message ? error.message : 'Sin conexión');
        }
    }

    cargar();
    setInterval(cargar, 20000);
})();
