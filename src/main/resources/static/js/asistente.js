(function () {
    "use strict";

    var MENSAJE_BIENVENIDA = "Hola, soy el asistente de Eclipse Suites.\n" +
        "Puedo ayudarte con tarifas, disponibilidad, servicios y políticas de estadía.\n" +
        "¿Qué te gustaría saber?";

    var SUGERENCIAS_INICIALES = [
        "¿Cuánto cuesta una suite?",
        "¿Qué suites están disponibles?",
        "¿Qué servicios incluyen?",
        "¿Cuál es el horario de check-in?",
        "Quiero hablar con recepción"
    ];

    var ETIQUETAS_MODO = {
        PUBLICO: "Modo visita · consultas guiadas",
        CLIENTE: "Asistente IA activo",
        CLIENTE_FALLBACK: "Asistente IA no disponible · respuestas guiadas"
    };

    var grid = document.getElementById("suites-grid");
    var resumenCatalogo = document.getElementById("catalogo-resumen");
    var buscarSuite = document.getElementById("buscar-suite");
    var filtroFecha = document.getElementById("filtro-fecha");
    var filtroHora = document.getElementById("filtro-hora");
    var botonAyuda = document.getElementById("boton-ayuda");

    var zonaMensajes = document.getElementById("chat-mensajes");
    var zonaSugerencias = document.getElementById("chat-sugerencias");
    var formulario = document.getElementById("chat-form");
    var entrada = document.getElementById("chat-entrada");
    var botonEnviar = document.getElementById("chat-enviar");
    var bannerCola = document.getElementById("chat-cola");
    var textoCola = document.getElementById("chat-cola-texto");
    var botonVolver = document.getElementById("chat-volver");
    var botonReiniciar = document.getElementById("chat-reiniciar");
    var etiquetaModo = document.getElementById("chat-modo");
    var toasts = document.getElementById("chat-toasts");
    var botonVerSuites = document.getElementById("chat-ver-suites");
    var panelSuites = document.getElementById("suites-panel");

    var tokenCsrf = null;
    var enviando = false;
    var tarjetas = [];

    /* ---------- Seguridad y utilidades ---------- */

    function escapar(valor) {
        return String(valor == null ? "" : valor)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function urlSegura(valor) {
        var url = String(valor || "").trim();
        if (/^\/[\w\-./]+$/.test(url)) {
            return url;
        }
        if (/^https?:\/\/[\w.\-]+(:\d+)?\/[\w\-./?=&%#]*$/.test(url)) {
            return url;
        }
        return "";
    }

    function formatear(texto) {
        var seguro = escapar(texto);
        return seguro
            .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
            .replace(/\n/g, "<br>");
    }

    function horaCorta(fecha) {
        try {
            return new Date(fecha).toLocaleTimeString("es-PE", {
                hour: "2-digit",
                minute: "2-digit"
            });
        } catch (e) {
            return "";
        }
    }

    /* ---------- Red ---------- */

    async function obtenerTokenCsrf() {
        if (tokenCsrf) {
            return tokenCsrf;
        }
        var respuesta = await fetch("/api/auth/csrf", {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store"
        });
        if (!respuesta.ok) {
            throw new Error("No se pudo preparar la protección de seguridad.");
        }
        var datos = await respuesta.json();
        tokenCsrf = datos.token;
        return tokenCsrf;
    }

    async function enviarJson(url, cuerpo, metodo) {
        var token = await obtenerTokenCsrf();
        var respuesta = await fetch(url, {
            method: metodo || "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": token
            },
            body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo)
        });

        var datos = {};
        try {
            datos = await respuesta.json();
        } catch (e) {
            datos = {};
        }

        if (!respuesta.ok) {
            var error = new Error(datos.mensaje || datos.error || "No se pudo completar la solicitud.");
            error.status = respuesta.status;
            throw error;
        }
        return datos;
    }

    async function obtenerJson(url) {
        var respuesta = await fetch(url, { credentials: "same-origin", cache: "no-store" });
        var datos = {};
        try {
            datos = await respuesta.json();
        } catch (e) {
            datos = {};
        }
        if (!respuesta.ok) {
            var error = new Error(datos.mensaje || datos.error || "No se pudo cargar la información.");
            error.status = respuesta.status;
            throw error;
        }
        return datos;
    }

    /* ---------- Catálogo ---------- */

    function renderTarjetas(lista) {
        tarjetas = Array.isArray(lista) ? lista : [];
        grid.innerHTML = "";

        if (!tarjetas.length) {
            var aviso = document.createElement("p");
            aviso.className = "suites-grid__estado";
            aviso.textContent = "No hay suites registradas por el momento.";
            grid.appendChild(aviso);
            resumenCatalogo.hidden = true;
            return;
        }

        tarjetas.forEach(function (habitacion) {
            var tarjeta = document.createElement("button");
            tarjeta.type = "button";
            tarjeta.className = "suite-card" + (habitacion.disponible ? "" : " suite-card--agotada");
            tarjeta.dataset.busqueda = [
                habitacion.numHabitacion,
                habitacion.nombreTematico,
                habitacion.descripcionTematica,
                habitacion.codTipo
            ].filter(Boolean).join(" ").toLowerCase();

            var fondo = urlSegura(habitacion.galeriaFotos);
            if (fondo) {
                tarjeta.style.backgroundImage = "url('" + fondo + "')";
            }

            var claseBadge = habitacion.disponible ? "" : " suite-card__badge--agotada";
            tarjeta.innerHTML =
                '<span class="suite-card__scrim"></span>' +
                '<span class="suite-card__title">Habitación ' +
                escapar(habitacion.numHabitacion) + "</span>" +
                '<span class="suite-card__badge' + claseBadge + '">' +
                '<span class="suite-card__badge-dot"></span>' +
                escapar(habitacion.badge) + "</span>";

            tarjeta.addEventListener("click", function () {
                abrirChat("¿Tiene disponibilidad la habitación " +
                    habitacion.numHabitacion + " (" + habitacion.nombreTematico + ")?");
            });

            grid.appendChild(tarjeta);
        });

        var libres = tarjetas.filter(function (h) { return h.disponible; }).length;
        resumenCatalogo.hidden = false;
        resumenCatalogo.textContent = libres + " de " + tarjetas.length +
            " suites disponibles ahora mismo.";
    }

    function filtrarTarjetas() {
        var termino = (buscarSuite.value || "").trim().toLowerCase();
        Array.from(grid.querySelectorAll(".suite-card")).forEach(function (tarjeta) {
            var coincide = !termino || (tarjeta.dataset.busqueda || "").indexOf(termino) !== -1;
            tarjeta.style.display = coincide ? "" : "none";
        });
    }

    async function cargarCatalogo() {
        try {
            var datos = await obtenerJson("/api/chatbot/catalogo");
            renderTarjetas(datos.habitaciones);
        } catch (e) {
            grid.innerHTML = '<p class="suites-grid__estado">No se pudo cargar el catálogo.</p>';
        }
    }

    /* ---------- Conversación ---------- */

    function agregarBurbuja(tipo, texto, fecha) {
        var burbuja = document.createElement("div");
        burbuja.className = "chat-burbuja chat-burbuja--" + tipo;
        burbuja.innerHTML = formatear(texto);
        if (fecha) {
            var hora = document.createElement("span");
            hora.className = "chat-burbuja__hora";
            hora.textContent = horaCorta(fecha);
            burbuja.appendChild(hora);
        }
        zonaMensajes.appendChild(burbuja);
        zonaMensajes.scrollTop = zonaMensajes.scrollHeight;
        return burbuja;
    }

    function agregarEscribiendo() {
        var indicador = document.createElement("div");
        indicador.className = "chat-escribiendo";
        indicador.setAttribute("aria-label", "El asistente está escribiendo");
        indicador.innerHTML = "<span></span><span></span><span></span>";
        zonaMensajes.appendChild(indicador);
        zonaMensajes.scrollTop = zonaMensajes.scrollHeight;
        return indicador;
    }

    function renderSugerencias(lista) {
        zonaSugerencias.innerHTML = "";
        var opciones = Array.isArray(lista) && lista.length ? lista : SUGERENCIAS_INICIALES;
        opciones.forEach(function (texto) {
            var chip = document.createElement("button");
            chip.type = "button";
            chip.className = "chip";
            chip.textContent = texto;
            chip.addEventListener("click", function () {
                enviarConsulta(texto);
            });
            zonaSugerencias.appendChild(chip);
        });
    }

    function mostrarModo(modo) {
        var etiqueta = ETIQUETAS_MODO[modo];
        if (!etiqueta) {
            etiquetaModo.hidden = true;
            return;
        }
        etiquetaModo.hidden = false;
        etiquetaModo.textContent = etiqueta;
    }

    function aplicarCola(datos) {
        var enCola = datos && datos.estadoCola && datos.estadoCola !== "SIN_COLA";
        bannerCola.hidden = !enCola;
        botonReiniciar.hidden = !enCola;
        if (!enCola) {
            return;
        }
        if (datos.estadoCola === "EN_COLA") {
            textoCola.textContent = "Estado: en cola · tiempo estimado 2 min (faltan " +
                datos.minutosRestantes + " min)";
        } else {
            textoCola.textContent =
                "Estado: en cola · un colaborador te responderá en breve";
        }
    }

    function toast(mensaje, ok) {
        var aviso = document.createElement("div");
        aviso.className = "chat-toast" + (ok ? " chat-toast--ok" : "");
        aviso.textContent = mensaje;
        toasts.appendChild(aviso);
        setTimeout(function () {
            aviso.remove();
        }, 4200);
    }

    async function cargarHistorico() {
        try {
            var datos = await obtenerJson("/api/chatbot/historico");
            mostrarModo(datos.modo);
            aplicarCola(datos);

            if (datos.mensajes && datos.mensajes.length) {
                datos.mensajes.forEach(function (mensaje) {
                    var tipo = mensaje.rol === "usuario" ? "usuario" : "bot";
                    agregarBurbuja(tipo, mensaje.contenido, mensaje.fhMensaje);
                });
                renderSugerencias(SUGERENCIAS_INICIALES);
            } else {
                agregarBurbuja("bot", MENSAJE_BIENVENIDA);
                renderSugerencias(SUGERENCIAS_INICIALES);
            }
        } catch (e) {
            agregarBurbuja("bot", MENSAJE_BIENVENIDA);
            renderSugerencias(SUGERENCIAS_INICIALES);
        }
    }

    async function enviarConsulta(texto) {
        var mensaje = String(texto || "").trim();
        if (!mensaje || enviando) {
            return;
        }

        agregarBurbuja("usuario", mensaje);
        entrada.value = "";
        enviando = true;
        botonEnviar.disabled = true;
        var indicador = agregarEscribiendo();

        try {
            var datos = await enviarJson("/api/chatbot/mensaje", { mensaje: mensaje });
            indicador.remove();
            agregarBurbuja("bot", datos.respuesta);
            mostrarModo(datos.modo);
            aplicarCola(datos);
            renderSugerencias(datos.sugerencias);
        } catch (error) {
            indicador.remove();
            if (error.status === 429) {
                agregarBurbuja("sistema", error.message);
                renderSugerencias([
                    "¿Cuánto cuesta una suite?",
                    "¿Qué suites están disponibles?",
                    "Quiero hablar con recepción"
                ]);
            } else {
                agregarBurbuja("error",
                    "No se pudo enviar tu mensaje. Vuelve a intentarlo en unos segundos.");
                toast("No se pudo enviar el mensaje");
            }
        } finally {
            enviando = false;
            botonEnviar.disabled = false;
        }
    }

    async function volverAlAsistente() {
        try {
            var datos = await enviarJson("/api/chatbot/reiniciar", {});
            aplicarCola(datos);
            agregarBurbuja("sistema", "Has vuelto al asistente. La derivación quedó cerrada.");
            renderSugerencias(SUGERENCIAS_INICIALES);
        } catch (error) {
            toast("No se pudo actualizar el estado");
        }
    }

    /* ---------- Acciones de la interfaz ---------- */

    function abrirChat(pregunta) {
        if (panelSuites) {
            panelSuites.classList.remove("catalogo-panel--abierto");
            if (botonVerSuites) {
                botonVerSuites.setAttribute("aria-expanded", "false");
            }
        }
        if (pregunta) {
            entrada.value = pregunta;
        }
        entrada.focus();
        zonaSugerencias.scrollIntoView({ block: "nearest" });
    }

    function preguntaConFecha() {
        var partes = [];
        if (filtroFecha.value) {
            var trozos = filtroFecha.value.split("-");
            partes.push("el " + trozos[2] + "/" + trozos[1] + "/" + trozos[0]);
        }
        if (filtroHora.value) {
            partes.push("a las " + filtroHora.value);
        }
        if (!partes.length) {
            return "";
        }
        return "¿Tienen disponibilidad " + partes.join(" ") + "?";
    }

    function conectarEventos() {
        buscarSuite.addEventListener("input", filtrarTarjetas);

        [filtroFecha, filtroHora].forEach(function (campo) {
            campo.addEventListener("change", function () {
                var pregunta = preguntaConFecha();
                if (pregunta) {
                    entrada.value = pregunta;
                    entrada.focus();
                }
            });
        });

        botonAyuda.addEventListener("click", function () {
            abrirChat("¿Qué puedo consultar contigo?");
        });

        document.getElementById("chat-nav-ayuda").addEventListener("click", function () {
            abrirChat("Necesito ayuda con mi estadía");
        });

        Array.from(document.querySelectorAll(".chat-nav__item[data-pregunta]"))
            .forEach(function (boton) {
                boton.addEventListener("click", function () {
                    abrirChat(boton.dataset.pregunta);
                });
            });

        formulario.addEventListener("submit", function (evento) {
            evento.preventDefault();
            enviarConsulta(entrada.value);
        });

        botonVolver.addEventListener("click", volverAlAsistente);
        botonReiniciar.addEventListener("click", volverAlAsistente);

        if (botonVerSuites) {
            botonVerSuites.addEventListener("click", function () {
                var abierto = panelSuites.classList.toggle("catalogo-panel--abierto");
                botonVerSuites.setAttribute("aria-expanded", abierto ? "true" : "false");
            });
        }
    }

    conectarEventos();
    renderSugerencias(SUGERENCIAS_INICIALES);
    cargarCatalogo();
    cargarHistorico();
})();
