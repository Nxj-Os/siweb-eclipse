(function () {
    'use strict';

    var galeria = document.getElementById('suite-gallery');
    var estado = document.getElementById('suites-status');
    var cta = document.getElementById('suites-cta');
    if (!galeria) return;

    var MAXIMO = 6;

    function escapar(valor) {
        return String(valor == null ? '' : valor)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function dinero(valor) {
        if (valor == null || isNaN(Number(valor))) return null;
        return 'S/ ' + Number(valor).toFixed(2);
    }

    function fallar(mensaje) {
        if (estado) estado.textContent = mensaje;
        galeria.innerHTML =
            '<p class="suites-showcase__empty">' +
            'No pudimos cargar las suites ahora mismo. ' +
            '<a href="/catalogo">Abre el catálogo</a> para ver todas y reservar.' +
            '</p>';
        if (cta) cta.hidden = false;
    }

    fetch('/api/catalogo', { credentials: 'same-origin' })
        .then(function (respuesta) {
            if (!respuesta.ok) throw new Error('catálogo no disponible');
            return respuesta.json();
        })
        .then(function (suites) {
            if (!Array.isArray(suites) || suites.length === 0) {
                fallar('No hay suites publicadas por el momento.');
                return;
            }

            var disponibles = suites.filter(function (s) { return s.disponible; }).length;
            var precios = suites
                .map(function (s) { return s.precioDesde == null ? null : Number(s.precioDesde); })
                .filter(function (p) { return p != null && !isNaN(p); });
            var minimo = precios.length ? Math.min.apply(null, precios) : null;

            if (estado) {
                var partes = [suites.length + (suites.length === 1 ? ' suite' : ' suites')];
                partes.push(disponibles + (disponibles === 1 ? ' disponible hoy' : ' disponibles hoy'));
                if (minimo != null) partes.push('desde S/ ' + minimo.toFixed(2));
                estado.textContent = partes.join(' · ');
            }

            var visibles = suites.slice(0, MAXIMO);
            galeria.innerHTML = visibles.map(function (suite, indice) {
                var codTipo = escapar(suite.codTipo || '');
                var href = '/catalogo/' + encodeURIComponent(suite.codTipo || '');
                var imagen = suite.imagen ? ' style="background-image:url(\'' + escapar(suite.imagen) + '\')"' : '';
                var precio = dinero(suite.precioDesde);
                var badge = suite.disponible
                    ? '<span class="suite-tile__badge">' +
                        (suite.habitacionesDisponibles || 0) + ' disponibles</span>'
                    : '<span class="suite-tile__badge suite-tile__badge--agotada">Sin disponibilidad</span>';

                return '' +
                    '<article class="suite-tile">' +
                        '<a class="suite-tile__image suite-tile__image--live"' + imagen +
                            ' href="' + href + '"' +
                            ' aria-label="Explorar ' + escapar(suite.nombre) + '">' +
                            '<span class="suite-tile__index">' +
                                String(indice + 1).padStart(2, '0') + ' / ' + codTipo +
                            '</span>' +
                            '<span class="suite-tile__arrow" aria-hidden="true">↗</span>' +
                        '</a>' +
                        '<div class="suite-tile__details">' +
                            '<div>' +
                                '<h3>' + escapar(suite.nombre) + '</h3>' +
                                '<p>' + escapar(suite.descripcion) + '</p>' +
                                badge +
                            '</div>' +
                            (precio
                                ? '<span class="suite-tile__price">desde<strong>' + precio + '</strong></span>'
                                : '') +
                        '</div>' +
                        '<a class="suite-tile__cta" href="' + href + '">' +
                            'Ver suite y reservar <span aria-hidden="true">→</span>' +
                        '</a>' +
                    '</article>';
            }).join('');

            if (cta) {
                cta.hidden = false;
                cta.innerHTML = 'Ver las ' + suites.length + ' suites y reservar ' +
                    '<span aria-hidden="true">↗</span>';
                cta.setAttribute('href', '/catalogo');
            }
        })
        .catch(function () {
            fallar('No pudimos cargar las suites ahora mismo.');
        });
})();
