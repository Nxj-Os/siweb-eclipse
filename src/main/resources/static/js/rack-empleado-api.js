document.addEventListener("DOMContentLoaded", async () => {
    const grid = document.getElementById("rooms");
    if (!grid) return;
    const texto = (valor) => String(valor ?? "");
    const clasificar = (estado) => {
        const e = texto(estado).toUpperCase();
        if (e.includes("LIMPI")) return "warn";
        if (e.includes("DISPON") || e.includes("LIBRE")) return "ok";
        return "busy";
    };
    const contar = (habitaciones, predicado) => habitaciones.filter(h => predicado(texto(h.estado).toUpperCase())).length;
    try {
        const respuesta = await fetch("/empleado/api/habitaciones", { credentials: "same-origin", cache: "no-store" });
        if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
        const habitaciones = await respuesta.json();
        document.getElementById("metric-disponibles").textContent = contar(habitaciones, e => e.includes("DISPON") || e.includes("LIBRE"));
        document.getElementById("metric-ocupadas").textContent = contar(habitaciones, e => e.includes("OCUP"));
        document.getElementById("metric-limpieza").textContent = contar(habitaciones, e => e.includes("LIMPI"));
        grid.replaceChildren();
        if (!habitaciones.length) { grid.textContent = "No hay habitaciones registradas."; return; }
        for (const h of habitaciones) {
            const tarjeta = document.createElement("article"); tarjeta.className = "room";
            tarjeta.dataset.search = [h.numero, h.tipo, h.estado].map(texto).join(" ").toLowerCase();
            const visual = document.createElement("div"); visual.className = "room-visual"; visual.textContent = texto(h.numero);
            const titulo = document.createElement("h3"); titulo.textContent = texto(h.tipo);
            const piso = document.createElement("p"); piso.textContent = `Hab. ${texto(h.numero)} · Piso ${texto(h.piso)}`;
            const estado = document.createElement("span"); estado.className = `status ${clasificar(h.estado)}`; estado.textContent = texto(h.estado);
            const boton = document.createElement("button"); boton.type = "button"; boton.textContent = "Ver detalles";
            boton.addEventListener("click", () => {
                const dialogo = document.getElementById("demo-dialog");
                document.getElementById("demo-message").textContent = `Habitación ${texto(h.numero)} · ${texto(h.tipo)} · Piso ${texto(h.piso)} · Estado: ${texto(h.estado)}`;
                dialogo?.showModal();
            });
            tarjeta.append(visual, titulo, piso, estado, boton); grid.append(tarjeta);
        }
        const filtro = document.querySelector('[data-filter="rooms"]');
        filtro?.dispatchEvent(new Event("input"));
    } catch (error) {
        grid.textContent = "No se pudieron cargar las habitaciones. Verifica la conexión y los permisos.";
        console.error("Rack:", error);
    }
});
