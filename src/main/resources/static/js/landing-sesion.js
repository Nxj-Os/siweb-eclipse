(function () {
    'use strict';

    function pintar(nombre) {
        var etiqueta = '<i class="bi bi-person" aria-hidden="true"></i> ' + nombre;
        var principal = document.getElementById('landing-login');
        var flotante = document.getElementById('landing-login-menu');

        if (principal) {
            principal.innerHTML = etiqueta + ' <span aria-hidden="true">↗</span>';
            principal.href = '/cliente/perfil';
        }
        if (flotante) {
            flotante.innerHTML = 'Mi perfil · ' + nombre + ' <span aria-hidden="true">↗</span>';
            flotante.href = '/cliente/perfil';
        }
    }

    fetch('/api/auth/client/perfil', { credentials: 'same-origin' })
        .then(function (respuesta) {
            if (!respuesta.ok) return null;
            return respuesta.json();
        })
        .then(function (perfil) {
            if (perfil && perfil.nombreCompleto) pintar(perfil.nombreCompleto);
        })
        .catch(function () { /* sin sesión: se conserva el enlace de acceso */ });

    var params = new URLSearchParams(window.location.search);
    if (params.get('cuenta') === 'eliminada') {
        var aviso = document.createElement('div');
        aviso.setAttribute('role', 'status');
        aviso.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:80;'
            + 'padding:0.85rem 1.2rem;background:#101918;color:#e5cf83;'
            + 'border-bottom:1px solid rgb(229 207 131 / 40%);text-align:center;'
            + 'font-family:"DM Sans",sans-serif;font-size:0.95rem;';
        aviso.textContent = params.get('mensaje') || 'Tu cuenta fue eliminada correctamente';
        document.body.appendChild(aviso);
        window.setTimeout(function () { aviso.remove(); }, 6000);
    }
})();
