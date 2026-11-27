document.addEventListener("DOMContentLoaded", () => {
    const grid = document.getElementById("rooms");
    const notice = document.getElementById("room-status-notice");
    const search = document.querySelector('[data-filter="rooms"]');
    if (!grid || !notice) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const estadosAnteriores = new Map();
    let cargando = false;
    let primeraCarga = true;

    const texto = (valor) => String(valor ?? "");
    const libre = (estado) => ["LIBRE", "DISPONIBLE"].includes(texto(estado).toUpperCase());
    const clasificar = (estado) => {
        const valor = texto(estado).toUpperCase();
        if (valor.includes("LIMPI")) return "warn";
        if (libre(valor)) return "ok";
        return "busy";
    };

    function filtrar() {
        const termino = texto(search?.value).trim().toLowerCase();
        grid.querySelectorAll(".room").forEach((tarjeta) => {
            tarjeta.hidden = !tarjeta.dataset.search.includes(termino);
        });
    }

    function pintar(habitaciones) {
        const estadoCuenta = (estado) => habitaciones.filter((h) =>
            texto(h.estado).toUpperCase() === estado).length;
        const disponibles = habitaciones.filter((h) => libre(h.estado)).length;
        const ocupadas = habitaciones.filter((h) =>
            texto(h.estado).toUpperCase() === "OCUPADA").length;
        const limpieza = habitaciones.filter((h) =>
            texto(h.estado).toUpperCase().includes("LIMPI")).length;
        document.getElementById("metric-total").textContent = String(habitaciones.length);
        document.getElementById("metric-disponibles").textContent = String(disponibles);
        document.getElementById("metric-bloqueadas").textContent = String(estadoCuenta("BLOQUEADA"));
        document.getElementById("metric-reservadas").textContent = String(estadoCuenta("RESERVADA"));
        document.getElementById("metric-ocupadas").textContent = String(ocupadas);
        document.getElementById("metric-limpieza").textContent = String(limpieza);
        document.getElementById("metric-mantenimiento").textContent = String(estadoCuenta("MANTENIMIENTO"));

        const tarjetas = habitaciones.map((habitacion) => {
            const tarjeta = document.createElement("article");
            tarjeta.className = "room";
            tarjeta.dataset.search = [
                habitacion.numero, habitacion.tipo, habitacion.estado, habitacion.motivoBloqueo
            ].map(texto).join(" ").toLowerCase();

            const visual = document.createElement("div");
            visual.className = "room-visual";
            visual.textContent = texto(habitacion.numero);

            const titulo = document.createElement("h3");
            titulo.textContent = texto(habitacion.tipo);

            const piso = document.createElement("p");
            piso.textContent = `Hab. ${texto(habitacion.numero)} · Piso ${texto(habitacion.piso)}`;

            const estado = document.createElement("span");
            estado.className = `status ${clasificar(habitacion.estado)}`;
            estado.textContent = texto(habitacion.estado);
            estado.setAttribute("aria-label", `Estado: ${texto(habitacion.estado)}`);

            tarjeta.append(visual, titulo, piso, estado);
            if (texto(habitacion.estado).toUpperCase() === "BLOQUEADA") {
                if (habitacion.bloqueoHasta) {
                    const expiracion = document.createElement("p");
                    expiracion.dataset.holdUntil = habitacion.bloqueoHasta;
                    expiracion.textContent = `Retención temporal · ${new Date(habitacion.bloqueoHasta).toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}`;
                    tarjeta.append(expiracion);
                }
                const motivo = document.createElement("p");
                motivo.textContent = `Motivo: ${texto(habitacion.motivoBloqueo) || "No indicado"}`;
                tarjeta.append(motivo);

                const liberar = document.createElement("button");
                liberar.type = "button";
                liberar.textContent = "Liberar habitación";
                liberar.addEventListener("click", () => cambiarBloqueo(habitacion, false));
                tarjeta.append(liberar);
            } else if (libre(habitacion.estado)) {
                const bloquear = document.createElement("button");
                bloquear.type = "button";
                bloquear.textContent = "Bloquear habitación";
                bloquear.addEventListener("click", () => cambiarBloqueo(habitacion, true));
                tarjeta.append(bloquear);
            }
            return tarjeta;
        });

        grid.replaceChildren(...tarjetas);
        if (!habitaciones.length) grid.textContent = "No hay habitaciones registradas.";
        filtrar();
    }

    async function cambiarBloqueo(habitacion, bloquear) {
        let motivo;
        if (bloquear) {
            motivo = window.prompt(`Motivo para bloquear la habitación ${habitacion.numero}:`);
            if (motivo === null) return;
            if (!motivo.trim()) {
                notice.textContent = "Debes indicar el motivo del bloqueo.";
                return;
            }
        } else if (!window.confirm(`¿Liberar la habitación ${habitacion.numero}?`)) {
            return;
        }

        try {
            const headers = { Accept: "application/json" };
            const opciones = { method: bloquear ? "POST" : "DELETE", credentials: "same-origin", headers };
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
            if (bloquear) {
                headers["Content-Type"] = "application/json";
                opciones.body = JSON.stringify({ motivo: motivo.trim() });
            }
            const respuesta = await fetch(
                `/empleado/api/habitaciones/${encodeURIComponent(habitacion.id)}/bloqueo`,
                opciones
            );
            const resultado = await respuesta.json();
            if (!respuesta.ok) throw new Error(resultado.mensaje || `Error HTTP ${respuesta.status}`);
            notice.textContent = bloquear
                ? `La habitación ${habitacion.numero} quedó bloqueada.`
                : `La habitación ${habitacion.numero} quedó liberada y disponible.`;
            await cargar();
        } catch (error) {
            notice.textContent = `No se pudo actualizar la habitación: ${error.message}`;
        }
    }

    async function cargar() {
        if (cargando) return;
        cargando = true;
        try {
            const respuesta = await fetch("/empleado/api/habitaciones", {
                credentials: "same-origin",
                cache: "no-store",
                headers: { Accept: "application/json" }
            });
            if (!respuesta.ok) throw new Error(`Error HTTP ${respuesta.status}`);
            const habitaciones = await respuesta.json();
            if (!Array.isArray(habitaciones)) throw new Error("La respuesta de habitaciones no es válida.");

            if (!primeraCarga) {
                habitaciones.forEach((habitacion) => {
                    const anterior = estadosAnteriores.get(habitacion.id);
                    if (anterior && anterior !== habitacion.estado) {
                        const situacion = texto(habitacion.estado).toUpperCase() === "BLOQUEADA"
                            ? "quedó bloqueada"
                            : libre(habitacion.estado) ? "quedó libre" : `cambió a ${habitacion.estado}`;
                        notice.textContent = `Aviso de recepción: la habitación ${habitacion.numero} ${situacion}.`;
                    }
                });
            }
            estadosAnteriores.clear();
            habitaciones.forEach((habitacion) => estadosAnteriores.set(habitacion.id, habitacion.estado));
            primeraCarga = false;
            pintar(habitaciones);
        } catch (error) {
            notice.textContent = `No se pudieron cargar las habitaciones: ${error.message}`;
            if (primeraCarga) grid.textContent = "Verifica la sesión y los permisos de recepción.";
        } finally {
            cargando = false;
        }
    }

    search?.addEventListener("input", filtrar);
    cargar();
    window.setInterval(cargar, 5000);
    window.setInterval(() => {
        document.querySelectorAll("[data-hold-until]").forEach((elemento) => {
            const restante = Math.max(0, new Date(elemento.dataset.holdUntil).getTime() - Date.now());
            const minutos = String(Math.floor(restante / 60000)).padStart(2, "0");
            const segundos = String(Math.floor((restante % 60000) / 1000)).padStart(2, "0");
            elemento.textContent = restante > 0
                ? `Retención temporal · ${minutos}:${segundos} restantes`
                : "Retención vencida · actualizando estado";
        });
    }, 1000);
});
