(function () {
    const lista = document.querySelector('#catalogo-lista');
    const buscador = document.querySelector('#catalogo-buscar');
    const contador = document.querySelector('#catalogo-contador');
    const etiquetaContador = document.querySelector('#catalogo-contador-label');
    const vacio = document.querySelector('#catalogo-vacio-js');
    const selectorOrden = document.querySelector('select[name="orden"]');
    const soloDisponibles = document.querySelector('input[name="soloDisponibles"]');

    const tarjetas = lista
        ? Array.from(lista.querySelectorAll('.catalogo-suite-card'))
        : [];

    tarjetas.forEach(function (tarjeta, indice) {
        tarjeta.dataset.ordenOriginal = String(indice);
    });

    function normalizar(valor) {
        return (valor || '').toString().toLowerCase();
    }

    function precio(tarjeta) {
        const valor = Number(tarjeta.dataset.price);
        return Number.isFinite(valor) && valor > 0 ? valor : Number.POSITIVE_INFINITY;
    }

    function aplicarFiltros() {
        if (!tarjetas.length) {
            return;
        }
        const termino = normalizar(buscador ? buscador.value.trim() : '');
        const soloConDisponibilidad = Boolean(soloDisponibles && soloDisponibles.checked);
        let visibles = 0;

        tarjetas.forEach(function (tarjeta) {
            const coincideTexto = !termino
                || normalizar(tarjeta.dataset.title).includes(termino)
                || normalizar(tarjeta.dataset.description).includes(termino)
                || normalizar(tarjeta.dataset.amenities).includes(termino);
            const disponible = Number(tarjeta.dataset.disponibles || 0) > 0;
            const mostrar = coincideTexto && (!soloConDisponibilidad || disponible);

            tarjeta.hidden = !mostrar;
            if (mostrar) {
                visibles += 1;
            }
        });

        if (contador) {
            contador.textContent = String(visibles);
        }
        if (etiquetaContador) {
            etiquetaContador.textContent = visibles === 1 ? 'suite encontrada' : 'suites encontradas';
        }
        if (vacio) {
            vacio.hidden = visibles !== 0;
        }
    }

    function ordenarTarjetas() {
        if (!tarjetas.length || !selectorOrden) {
            return;
        }
        const criterio = selectorOrden.value;
        const ordenadas = tarjetas.slice().sort(function (a, b) {
            if (criterio === 'precio-asc') {
                return precio(a) - precio(b);
            }
            if (criterio === 'precio-desc') {
                return precio(b) - precio(a);
            }
            if (criterio === 'disponibilidad') {
                return Number(b.dataset.disponibles || 0) - Number(a.dataset.disponibles || 0);
            }
            if (criterio === 'nombre') {
                return normalizar(a.dataset.title).localeCompare(normalizar(b.dataset.title));
            }
            return Number(a.dataset.ordenOriginal) - Number(b.dataset.ordenOriginal);
        });

        ordenadas.forEach(function (tarjeta, indice) {
            tarjeta.style.order = String(indice);
        });
    }

    if (buscador) {
        buscador.addEventListener('input', function () {
            aplicarFiltros();
            const parametros = new URLSearchParams(window.location.search);
            if (buscador.value.trim()) {
                parametros.set('q', buscador.value.trim());
            } else {
                parametros.delete('q');
            }
            const nuevaUrl = parametros.toString();
            window.history.replaceState(null, '',
                window.location.pathname + (nuevaUrl ? '?' + nuevaUrl : ''));
        });
    }

    if (soloDisponibles) {
        soloDisponibles.addEventListener('change', aplicarFiltros);
    }

    if (selectorOrden) {
        selectorOrden.addEventListener('change', ordenarTarjetas);
    }

    document.querySelectorAll('img[data-fallback]').forEach(function (imagen) {
        imagen.addEventListener('error', function () {
            const respaldo = imagen.dataset.fallback;
            if (respaldo && imagen.src.indexOf(respaldo) === -1) {
                imagen.src = respaldo;
            }
        });
    });

    ordenarTarjetas();
    aplicarFiltros();
})();
