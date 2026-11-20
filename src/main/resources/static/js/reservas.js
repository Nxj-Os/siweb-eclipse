(() => {
    const form = document.querySelector(".reserva-form");
    if (!form) return;

    const roomOptions = [...form.querySelectorAll(".room-option")];
    const roomInputs = roomOptions.map((option) => option.querySelector('input[name="habitacionId"]'));
    const modalityInputs = [...form.querySelectorAll('input[name="modalidadId"]')];
    const dateInput = form.querySelector('input[name="fechaReserva"]');
    const timeInput = form.querySelector('input[name="horaInicio"]');
    const guestInput = form.querySelector('input[name="cantidadPersonas"]');
    const searchInput = document.querySelector("#buscar-habitacion");
    const summary = document.querySelector("#disponibilidad-resumen");
    const priceOutput = document.querySelector("#tarifa-seleccionada");
    const noResults = document.querySelector("#sin-resultados");
    const includes = document.querySelector("#preview-includes");
    const tipoId = form.dataset.tipoId;
    const rates = [...document.querySelectorAll("#tarifa-data li")];
    let availabilityRequest = 0;

    const selectedRoom = () => roomInputs.find((input) => input.checked);
    const selectedModality = () => modalityInputs.find((input) => input.checked);

    function updatePreview() {
        const room = selectedRoom();
        const modality = selectedModality();
        const date = dateInput.value;
        const time = timeInput.value;

        document.querySelector("#preview-room").textContent =
            room ? `Habitación ${room.closest(".room-option").dataset.roomNumber}` : "Por elegir";
        document.querySelector("#preview-guests").textContent = guestInput.value || "1";
        document.querySelector("#preview-duration").textContent =
            modality ? modality.dataset.nombre : "Por elegir";
        const features = (includes.dataset.incluye || "").split("|").filter(Boolean);
        if (features.length) {
            includes.replaceChildren(...features.map((feature) => {
                const tag = document.createElement("span");
                tag.textContent = feature;
                return tag;
            }));
        }

        if (!date || !time) {
            document.querySelector("#preview-start").textContent = "Por elegir";
            document.querySelector("#preview-end").textContent = "Por elegir";
            return;
        }

        const start = new Date(`${date}T${time}`);
        const end = new Date(start.getTime() + Number(modality?.dataset.duracionMinutos || 0) * 60000);
        document.querySelector("#preview-start").textContent =
            start.toLocaleString("es-PE", { dateStyle: "medium", timeStyle: "short" });
        document.querySelector("#preview-end").textContent = modality
            ? end.toLocaleString("es-PE", { dateStyle: "medium", timeStyle: "short" })
            : "Elige una duración";
    }

    function updatePrice() {
        const modality = selectedModality();
        if (!modality) {
            priceOutput.textContent = "Selecciona la duración para consultar el precio.";
            return;
        }
        const nowDate = dateInput.value;
        const rate = rates.find((item) => {
            if (item.dataset.tipo !== tipoId || item.dataset.modalidad !== modality.value) return false;
            return (!item.dataset.desde || item.dataset.desde <= nowDate)
                && (!item.dataset.hasta || item.dataset.hasta >= nowDate);
        });
        priceOutput.textContent = rate
            ? `Total de la estadía: S/ ${Number(rate.dataset.precio).toFixed(2)}`
            : "No hay una tarifa configurada para esa duración y fecha.";
    }

    function updateSearch() {
        const query = (searchInput.value || "").trim().toLowerCase();
        let visibleCount = 0;
        roomOptions.forEach((option) => {
            const matches = option.dataset.roomNumber.toLowerCase().includes(query);
            option.hidden = !matches;
            if (matches) visibleCount += 1;
        });
        noResults.hidden = visibleCount > 0;
    }

    async function refreshAvailability() {
        const requestId = ++availabilityRequest;
        if (!dateInput.value || !timeInput.value || !selectedModality()) {
            summary.textContent = "Elige fecha, hora y duración para consultar disponibilidad.";
            return;
        }
        if (new Date(`${dateInput.value}T${timeInput.value}`) <= new Date()) {
            roomInputs.forEach((input) => {
                input.disabled = true;
                input.checked = false;
            });
            summary.textContent = "Selecciona una fecha y hora futuras para consultar disponibilidad.";
            updatePreview();
            return;
        }
        const params = new URLSearchParams({
            tipoHabitacionId: tipoId,
            fechaReserva: dateInput.value,
            horaInicio: timeInput.value,
            modalidadId: selectedModality().value
        });
        summary.textContent = "Consultando disponibilidad…";
        try {
            const response = await fetch(`${form.dataset.disponibilidadUrl}?${params}`, {
                headers: { Accept: "application/json" },
                credentials: "same-origin"
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const availableIds = new Set((await response.json()).map(String));
            if (requestId !== availabilityRequest) return;

            let availableCount = 0;
            roomOptions.forEach((option, index) => {
                const input = roomInputs[index];
                const available = availableIds.has(String(input.value));
                if (available) availableCount += 1;
                option.classList.toggle("room-option--unavailable", !available);
                option.querySelector("[data-room-status]").textContent = available ? "Disponible" : "Ocupada";
                input.disabled = !available;
                if (!available && input.checked) input.checked = false;
            });
            summary.textContent = availableCount
                ? `${availableCount} habitación${availableCount === 1 ? "" : "es"} disponible${availableCount === 1 ? "" : "s"} para este horario.`
                : "No hay habitaciones disponibles para este horario.";
            updatePreview();
        } catch (error) {
            if (requestId !== availabilityRequest) return;
            roomInputs.forEach((input) => {
                input.disabled = true;
                input.checked = false;
            });
            summary.textContent = "No se pudo consultar la disponibilidad. Revisa tu conexión e inténtalo nuevamente.";
            console.error("Error al consultar disponibilidad de habitaciones:", error);
        }
    }

    roomInputs.forEach((input) => input.addEventListener("change", updatePreview));
    modalityInputs.forEach((input) => input.addEventListener("change", () => {
        updatePreview();
        updatePrice();
        refreshAvailability();
    }));
    [dateInput, timeInput].forEach((input) => input.addEventListener("change", () => {
        updatePreview();
        updatePrice();
        refreshAvailability();
    }));
    guestInput.addEventListener("input", updatePreview);
    searchInput.addEventListener("input", updateSearch);

    form.addEventListener("submit", (event) => {
        const room = selectedRoom();
        if (!room || room.disabled) {
            event.preventDefault();
            summary.textContent = "Selecciona una habitación disponible para continuar.";
            form.querySelector(".reserva-panel").scrollIntoView({ behavior: "smooth", block: "start" });
        }
    });

    document.querySelectorAll("img[data-fallback]").forEach((image) => {
        image.addEventListener("error", () => {
            if (image.src !== new URL(image.dataset.fallback, window.location.origin).href) {
                image.src = image.dataset.fallback;
            }
        }, { once: true });
    });

    updateSearch();
    updatePreview();
    updatePrice();
    refreshAvailability();
    window.setInterval(refreshAvailability, 15000);
})();
