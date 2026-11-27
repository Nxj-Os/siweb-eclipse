(function () {
    'use strict';

    var BASE = '/api/auth/client';
    var seccionActual = 'info';

    function token() {
        var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    function json(url, opciones) {
        var opcionesFinales = Object.assign({
            credentials: 'same-origin',
            headers: { 'X-XSRF-TOKEN': token() }
        }, opciones || {});
        return fetch(url, opcionesFinales).then(function (respuesta) {
            return respuesta.json().catch(function () { return {}; }).then(function (cuerpo) {
                if (!respuesta.ok) {
                    var error = new Error(cuerpo.mensaje || 'No se pudo completar la operación');
                    error.detalles = cuerpo.detalles || {};
                    throw error;
                }
                return cuerpo;
            });
        });
    }

    function irA(seccion) {
        seccionActual = seccion;
        document.querySelectorAll('.perfil-panel').forEach(function (panel) {
            panel.hidden = panel.getAttribute('data-panel') !== seccion;
        });
        document.querySelectorAll('.perfil-menu__item').forEach(function (item) {
            item.classList.toggle('is-active', item.getAttribute('data-ir') === seccion);
        });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    document.addEventListener('click', function (evento) {
        var destino = evento.target.closest('[data-ir]');
        if (destino) {
            evento.preventDefault();
            irA(destino.getAttribute('data-ir'));
        }
    });

    /* ---------------- aviso ---------------- */
    function aviso(elemento, texto, esError) {
        if (!elemento) return;
        elemento.hidden = !texto;
        elemento.textContent = texto || '';
        elemento.classList.toggle('is-error', !!esError);
    }

    function limpiarErrores(formulario) {
        formulario.querySelectorAll('[data-error-for]').forEach(function (nodo) {
            nodo.textContent = '';
        });
        formulario.querySelectorAll('input').forEach(function (campo) {
            campo.removeAttribute('aria-invalid');
        });
    }

    function pintarErrores(formulario, detalles) {
        Object.keys(detalles).forEach(function (campo) {
            var nodo = formulario.querySelector('[data-error-for="' + campo + '"]');
            if (nodo) {
                nodo.textContent = detalles[campo];
                var input = formulario.querySelector('#' + campo);
                if (input) input.setAttribute('aria-invalid', 'true');
            }
        });
    }

    /* ---------------- formulario ---------------- */
    var formulario = document.getElementById('form-perfil');
    if (formulario) {
        var avisoPerfil = document.getElementById('aviso-perfil');
        formulario.addEventListener('submit', function (evento) {
            evento.preventDefault();
            limpiarErrores(formulario);
            aviso(avisoPerfil, '', false);

            var datos = {
                nombre: formulario.nombre.value.trim(),
                apellido: formulario.apellido.value.trim(),
                telefono: formulario.telefono.value.trim(),
                edad: Number(formulario.edad.value)
            };

            var boton = formulario.querySelector('button[type="submit"]');
            boton.disabled = true;

            json(BASE + '/perfil', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token() },
                body: JSON.stringify(datos)
            }).then(function (perfil) {
                aviso(avisoPerfil, 'Datos guardados correctamente.', false);
                document.querySelectorAll('.perfil-hero__name').forEach(function (nodo) {
                    nodo.textContent = perfil.nombreCompleto;
                });
                document.querySelectorAll('.perfil-card--identidad h2').forEach(function (nodo) {
                    nodo.textContent = perfil.nombreCompleto;
                });
                document.querySelectorAll('.perfil-avatar').forEach(function (nodo) {
                    nodo.textContent = (perfil.nombre || '?').charAt(0).toUpperCase();
                });
            }).catch(function (error) {
                if (error.detalles && Object.keys(error.detalles).length) {
                    pintarErrores(formulario, error.detalles);
                    aviso(avisoPerfil, 'Revisa los campos marcados.', true);
                } else {
                    aviso(avisoPerfil, error.message, true);
                }
            }).finally(function () {
                boton.disabled = false;
            });
        });
    }

    /* ---------------- eliminar cuenta ---------------- */
    var confirmacion = document.getElementById('confirmacion');
    var btnEliminar = document.getElementById('btn-eliminar');
    if (confirmacion && btnEliminar) {
        confirmacion.addEventListener('input', function () {
            btnEliminar.disabled = confirmacion.value.trim().toUpperCase() !== 'ELIMINAR';
        });

        btnEliminar.addEventListener('click', function () {
            var avisoEliminar = document.getElementById('aviso-eliminar');
            if (!window.confirm('¿Seguro que deseas eliminar tu cuenta? Esta acción no se puede deshacer.')) {
                return;
            }
            btnEliminar.disabled = true;
            aviso(avisoEliminar, '', false);

            json(BASE + '/cuenta', { method: 'DELETE' })
                .then(function (respuesta) {
                    window.location.href = '/?cuenta=eliminada&mensaje='
                        + encodeURIComponent(respuesta.mensaje || 'Cuenta eliminada');
                })
                .catch(function (error) {
                    aviso(avisoEliminar, error.message, true);
                    btnEliminar.disabled = confirmacion.value.trim().toUpperCase() !== 'ELIMINAR';
                });
        });
    }

    /* ---------------- boleta ---------------- */
    var overlay = document.getElementById('boleta-overlay');
    var datosBoleta = document.getElementById('boleta-datos');
    var totalesBoleta = document.getElementById('boleta-totales');

    function cerrarBoleta() {
        if (overlay) overlay.hidden = true;
    }

    function dinero(valor) {
        return 'S/. ' + Number(valor || 0).toFixed(2);
    }

    function fila(contenido, esTotal) {
        return '<tr' + (esTotal ? ' class="is-total"' : '') + '><td colspan="2">' + contenido + '</td></tr>';
    }

    function par(etiqueta, valor) {
        return '<dt>' + etiqueta + '</dt><dd>' + (valor == null || valor === '' ? '—' : valor) + '</dd>';
    }

    function cargarBoleta(id) {
        json(BASE + '/reservas/' + id + '/boleta').then(function (boleta) {
            datosBoleta.innerHTML =
                par('N° boleta', boleta.numeroBoleta) +
                par('Cliente', boleta.cliente) +
                par('Documento', boleta.tipoDocumento + ' ' + boleta.numDocumento) +
                par('Correo', boleta.correoElectronico) +
                par('Suite', boleta.suite) +
                par('Habitación', boleta.habitacion) +
                par('Turno', boleta.modalidad) +
                par('Ingreso', boleta.fechaInicio) +
                par('Salida', boleta.fechaFin) +
                par('Estado', boleta.estado) +
                par('Método de pago', boleta.metodoPago) +
                par('Personas', boleta.cantidadPersonas) +
                par('Emisión', boleta.fechaEmision);

            totalesBoleta.innerHTML =
                '<tr><td>Subtotal</td><td>' + dinero(boleta.subtotal) + '</td></tr>' +
                '<tr><td>IGV (18%)</td><td>' + dinero(boleta.igv) + '</td></tr>' +
                '<tr><td>Valor total</td><td>' + dinero(boleta.total) + '</td></tr>';

            overlay.hidden = false;
        }).catch(function (error) {
            window.alert(error.message);
        });
    }

    document.addEventListener('click', function (evento) {
        var boton = evento.target.closest('[data-boleta]');
        if (boton) {
            cargarBoleta(boton.getAttribute('data-boleta'));
        }
    });

    var btnCerrar = document.getElementById('boleta-cerrar');
    var btnVolver = document.getElementById('boleta-volver');
    if (btnCerrar) btnCerrar.addEventListener('click', cerrarBoleta);
    if (btnVolver) btnVolver.addEventListener('click', cerrarBoleta);
    if (overlay) {
        overlay.addEventListener('click', function (evento) {
            if (evento.target === overlay) cerrarBoleta();
        });
    }
    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape' && overlay && !overlay.hidden) cerrarBoleta();
    });
})();
