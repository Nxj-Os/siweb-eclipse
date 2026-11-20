(function () {
    "use strict";

    var BIENVENIDA = "Hola, soy el asistente de Eclipse Suites.\n" +
        "Puedo ayudarte con tarifas, disponibilidad, servicios y políticas de estadía.\n" +
        "¿Qué te gustaría saber?";

    var SUGERENCIAS = [
        "¿Cuánto cuesta una suite?",
        "¿Qué suites están disponibles?",
        "¿Qué servicios incluyen?",
        "Quiero hablar con recepción"
    ];

    var ETIQUETAS_MODO = {
        PUBLICO: "Modo visita · consultas guiadas",
        CLIENTE: "Asistente IA activo",
        CLIENTE_FALLBACK: "Asistente IA no disponible · respuestas guiadas"
    };

    var raiz = document.getElementById("eclipse-chat");
    if (raiz) {
        return;
    }

    inyectarEstilos();

    raiz = document.createElement("div");
    raiz.id = "eclipse-chat";
    raiz.innerHTML = [
        '<button type="button" class="eclipse-chat-fab" id="eclipse-chat-fab" ',
        'aria-controls="eclipse-chat-panel" aria-expanded="false">',
        '<span class="eclipse-chat-fab__punto" aria-hidden="true"></span>',
        '<span>Asistente</span>',
        "</button>",
        '<aside class="eclipse-chat-panel" id="eclipse-chat-panel" role="dialog" ',
        'aria-label="Asistente IA" aria-hidden="true">',
        '<header class="eclipse-chat-cabecera">',
        "<div>",
        '<h2 class="eclipse-chat-cabecera__titulo">Asistente IA</h2>',
        '<p class="eclipse-chat-cabecera__sub" id="eclipse-chat-modo" hidden></p>',
        "</div>",
        '<button type="button" class="eclipse-chat-cerrar" id="eclipse-chat-cerrar" ',
        'aria-label="Cerrar asistente">&times;</button>',
        "</header>",
        '<div class="eclipse-chat-mensajes" id="eclipse-chat-mensajes" role="log" ',
        'aria-live="polite"></div>',
        '<div class="eclipse-chat-cola" id="eclipse-chat-cola" hidden>',
        '<span id="eclipse-chat-cola-texto"></span>',
        '<button type="button" class="eclipse-chat-cola__accion" ',
        'id="eclipse-chat-volver">Volver al asistente</button>',
        "</div>",
        '<div class="eclipse-chat-sugerencias" id="eclipse-chat-sugerencias"></div>',
        '<form class="eclipse-chat-form" id="eclipse-chat-form" autocomplete="off">',
        '<input class="eclipse-chat-input" id="eclipse-chat-input" type="text" ',
        'placeholder="Escribe tu consulta" maxlength="600" aria-label="Escribe tu consulta">',
        '<button class="eclipse-chat-enviar" id="eclipse-chat-enviar" type="submit" ',
        'aria-label="Enviar mensaje">&#10148;</button>',
        "</form>",
        '<p class="eclipse-chat-pie">Para más ayuda, contacta con recepción</p>',
        "</aside>"
    ].join("");
    document.body.appendChild(raiz);

    var fab = document.getElementById("eclipse-chat-fab");
    var panel = document.getElementById("eclipse-chat-panel");
    var cerrar = document.getElementById("eclipse-chat-cerrar");
    var zonaMensajes = document.getElementById("eclipse-chat-mensajes");
    var zonaSugerencias = document.getElementById("eclipse-chat-sugerencias");
    var formulario = document.getElementById("eclipse-chat-form");
    var entrada = document.getElementById("eclipse-chat-input");
    var botonEnviar = document.getElementById("eclipse-chat-enviar");
    var bannerCola = document.getElementById("eclipse-chat-cola");
    var textoCola = document.getElementById("eclipse-chat-cola-texto");
    var botonVolver = document.getElementById("eclipse-chat-volver");
    var etiquetaModo = document.getElementById("eclipse-chat-modo");

    var tokenCsrf = null;
    var abierto = false;
    var cargado = false;
    var enviando = false;

    /* ---------- Seguridad y utilidades ---------- */

    function escapar(valor) {
        return String(valor == null ? "" : valor)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function formatear(texto) {
        return escapar(texto)
            .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
            .replace(/\n/g, "<br>");
    }

    function inyectarEstilos() {
        if (document.querySelector('link[data-eclipse-chat]')) {
            return;
        }
        var enlace = document.createElement("link");
        enlace.rel = "stylesheet";
        enlace.href = "/css/chatbot-widget.css";
        enlace.setAttribute("data-eclipse-chat", "true");
        document.head.appendChild(enlace);
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

    async function enviarJson(url, cuerpo) {
        var token = await obtenerTokenCsrf();
        var respuesta = await fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": token
            },
            body: JSON.stringify(cuerpo || {})
        });
        var datos = {};
        try {
            datos = await respuesta.json();
        } catch (e) {
            datos = {};
        }
        if (!respuesta.ok) {
            var error = new Error(datos.mensaje || datos.error ||
                "No se pudo completar la solicitud.");
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
            throw new Error(datos.mensaje || "No se pudo cargar la información.");
        }
        return datos;
    }

    /* ---------- Conversación ---------- */

    function agregarBurbuja(tipo, texto) {
        var burbuja = document.createElement("div");
        burbuja.className = "eclipse-chat-burbuja eclipse-chat-burbuja--" + tipo;
        burbuja.innerHTML = formatear(texto);
        zonaMensajes.appendChild(burbuja);
        zonaMensajes.scrollTop = zonaMensajes.scrollHeight;
        return burbuja;
    }

    function agregarEscribiendo() {
        var indicador = document.createElement("div");
        indicador.className = "eclipse-chat-escribiendo";
        indicador.setAttribute("aria-label", "El asistente está escribiendo");
        indicador.innerHTML = "<span></span><span></span><span></span>";
        zonaMensajes.appendChild(indicador);
        zonaMensajes.scrollTop = zonaMensajes.scrollHeight;
        return indicador;
    }

    function renderSugerencias(lista) {
        zonaSugerencias.innerHTML = "";
        var opciones = Array.isArray(lista) && lista.length ? lista : SUGERENCIAS;
        opciones.forEach(function (texto) {
            var chip = document.createElement("button");
            chip.type = "button";
            chip.className = "eclipse-chat-chip";
            chip.textContent = texto;
            chip.addEventListener("click", function () {
                enviarConsulta(texto);
            });
            zonaSugerencias.appendChild(chip);
        });
    }

    function mostrarModo(modo) {
        var etiqueta = ETIQUETAS_MODO[modo];
        etiquetaModo.hidden = !etiqueta;
        if (etiqueta) {
            etiquetaModo.textContent = etiqueta;
        }
    }

    function aplicarCola(datos) {
        var enCola = datos && datos.estadoCola && datos.estadoCola !== "SIN_COLA";
        bannerCola.hidden = !enCola;
        if (!enCola) {
            return;
        }
        textoCola.textContent = datos.estadoCola === "EN_COLA"
            ? "Estado: en cola · tiempo estimado 2 min (faltan " +
            datos.minutosRestantes + " min)"
            : "Estado: en cola · un colaborador te responderá en breve";
    }

    async function cargarHistorico() {
        if (cargado) {
            return;
        }
        cargado = true;
        try {
            var datos = await obtenerJson("/api/chatbot/historico");
            mostrarModo(datos.modo);
            aplicarCola(datos);
            if (datos.mensajes && datos.mensajes.length) {
                datos.mensajes.forEach(function (mensaje) {
                    agregarBurbuja(mensaje.rol === "usuario" ? "usuario" : "bot",
                        mensaje.contenido);
                });
            } else {
                agregarBurbuja("bot", BIENVENIDA);
            }
        } catch (e) {
            agregarBurbuja("bot", BIENVENIDA);
        }
        renderSugerencias(SUGERENCIAS);
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
                renderSugerencias(SUGERENCIAS);
            } else {
                agregarBurbuja("error",
                    "No se pudo enviar tu mensaje. Vuelve a intentarlo en unos segundos.");
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
            renderSugerencias(SUGERENCIAS);
        } catch (error) {
            agregarBurbuja("error", "No se pudo actualizar el estado de la derivación.");
        }
    }

    /* ---------- Interfaz ---------- */

    function alternar(valor) {
        abierto = typeof valor === "boolean" ? valor : !abierto;
        panel.classList.toggle("eclipse-chat-panel--abierto", abierto);
        panel.setAttribute("aria-hidden", abierto ? "false" : "true");
        fab.setAttribute("aria-expanded", abierto ? "true" : "false");
        if (abierto) {
            cargarHistorico();
            entrada.focus();
        }
    }

    fab.addEventListener("click", function () {
        alternar();
    });

    cerrar.addEventListener("click", function () {
        alternar(false);
    });

    document.addEventListener("keydown", function (evento) {
        if (evento.key === "Escape" && abierto) {
            alternar(false);
        }
    });

    formulario.addEventListener("submit", function (evento) {
        evento.preventDefault();
        enviarConsulta(entrada.value);
    });

    botonVolver.addEventListener("click", volverAlAsistente);

    renderSugerencias(SUGERENCIAS);
})();
