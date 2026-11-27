(() => {
    'use strict';

    const $ = (s) => document.querySelector(s);
    const esc = (v) => String(v == null ? '' : v)
        .replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    const SIGUIENTE = {
        RECIBIDO: 'EN_PREPARACION',
        EN_PREPARACION: 'CONTROLADO',
        CONTROLADO: 'ENTREGADO',
        ENTREGADO: null
    };
    const ETIQUETA = {
        RECIBIDO: 'Recibido',
        EN_PREPARACION: 'En preparación',
        CONTROLADO: 'Controlado',
        ENTREGADO: 'Entregado'
    };

    let pedidos = [];
    let filtro = '';

    function tokenCsrf() {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : null;
    }

    function hora(iso) {
        if (!iso) return '—';
        let fecha;
        if (Array.isArray(iso)) {
            fecha = new Date(iso[0], (iso[1] || 1) - 1, iso[2] || 1, iso[3] || 0, iso[4] || 0, iso[5] || 0);
        } else {
            fecha = new Date(iso);
        }
        if (isNaN(fecha.getTime())) return '—';
        return fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    }

    function contenido(pedido) {
        if (!pedido.items || !pedido.items.length) return '—';
        return pedido.items.map((i) => i.cantidad + '× ' + i.nombre).join(', ');
    }

    function pintarMetricas() {
        const total = pedidos.length;
        const enCurso = pedidos.filter((p) => p.estado === 'RECIBIDO' || p.estado === 'EN_PREPARACION').length;
        const entregadas = pedidos.filter((p) => p.estado === 'ENTREGADO').length;
        $('#metric-total').textContent = total;
        $('#metric-en-curso').textContent = enCurso;
        $('#metric-entregadas').textContent = entregadas;
    }

    function pintarTabla() {
        const tbody = $('#comandas-tbody');
        const vacias = $('#comandas-vacias');
        const visibles = pedidos.filter((p) => {
            if (!filtro) return true;
            const texto = (p.codigo + ' ' + (p.numHabitacion || '') + ' ' + contenido(p)).toLowerCase();
            return texto.indexOf(filtro) !== -1;
        });

        if (!visibles.length) {
            tbody.innerHTML = '<tr><td colspan="7">' + (pedidos.length ? 'Sin resultados para la búsqueda.' : 'Aún no hay comandas.') + '</td></tr>';
            vacias.hidden = pedidos.length > 0;
            return;
        }
        vacias.hidden = true;
        tbody.innerHTML = visibles.map((p) => {
            const sig = SIGUIENTE[p.estado];
            const accion = sig
                ? '<button type="button" data-codigo="' + esc(p.codigo) + '" data-estado="' + esc(sig) + '">'
                + esc(ETIQUETA[sig]) + '</button>'
                : '<span class="rs-contenido">Cerrada</span>';
            return '<tr>'
                + '<td><b>' + esc(p.codigo) + '</b></td>'
                + '<td>' + esc(p.numHabitacion || '—') + '</td>'
                + '<td class="rs-contenido">' + esc(contenido(p)) + '</td>'
                + '<td>S/ ' + esc(Number(p.montoTotal).toFixed(2)) + '</td>'
                + '<td>' + esc(hora(p.fHPedido)) + '</td>'
                + '<td><span class="status rs-estado--' + esc(p.estado) + '">' + esc(ETIQUETA[p.estado] || p.estado) + '</span></td>'
                + '<td><div class="rs-acciones">' + accion + '</div></td>'
                + '</tr>';
        }).join('');
    }

    async function cargar() {
        const respuesta = await fetch('/api/roomservice/pedidos', { headers: { Accept: 'application/json' } });
        if (!respuesta.ok) {
            $('#comandas-tbody').innerHTML = '<tr><td colspan="7">No se pudieron cargar las comandas ('
                + respuesta.status + ').</td></tr>';
            return;
        }
        pedidos = await respuesta.json();
        if (!Array.isArray(pedidos)) pedidos = [];
        pintarMetricas();
        pintarTabla();
    }

    async function avanzar(codigo, estado) {
        const token = tokenCsrf();
        const respuesta = await fetch('/api/roomservice/pedidos/' + encodeURIComponent(codigo) + '/estado', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-XSRF-TOKEN': token || ''
            },
            body: JSON.stringify({ estado: estado })
        });
        if (!respuesta.ok) {
            const texto = await respuesta.text();
            alert('No se pudo actualizar la comanda: ' + (texto || respuesta.status));
        }
        await cargar();
    }

    document.addEventListener('click', (evento) => {
        const boton = evento.target.closest('button[data-codigo][data-estado]');
        if (boton) {
            boton.disabled = true;
            avanzar(boton.dataset.codigo, boton.dataset.estado);
            return;
        }
        if (evento.target.closest('#btn-actualizar')) {
            cargar();
        }
    });

    document.addEventListener('input', (evento) => {
        if (evento.target.id === 'comanda-busqueda') {
            filtro = evento.target.value.trim().toLowerCase();
            pintarTabla();
        }
    });

    cargar();
    setInterval(cargar, 30000);
})();
