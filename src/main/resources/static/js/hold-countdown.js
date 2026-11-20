(() => {
    const card = document.querySelector("[data-expira]");
    if (!card) return;

    const output = document.querySelector("#hold-countdown");
    const message = document.querySelector("#hold-message");
    const form = document.querySelector(".hold-payment-form");
    const expiration = Date.parse(card.dataset.expira);
    let timer;
    if (!Number.isFinite(expiration)) {
        output.textContent = "--:--";
        message.textContent = "No se pudo determinar el tiempo restante. Actualiza la página o vuelve a tus reservas.";
        form.querySelectorAll("input, button[type='submit']").forEach((control) => {
            control.disabled = true;
        });
        console.error("La fecha de expiración de la retención no es válida.");
        return;
    }

    function updateCountdown() {
        const remaining = Math.max(0, Math.ceil((expiration - Date.now()) / 1000));
        const minutes = Math.floor(remaining / 60).toString().padStart(2, "0");
        const seconds = (remaining % 60).toString().padStart(2, "0");
        output.textContent = `${minutes}:${seconds}`;
        if (remaining === 0) {
            message.textContent = "El tiempo para pagar terminó. La habitación quedó disponible nuevamente.";
            form.querySelectorAll("input, button[type='submit']").forEach((control) => {
                control.disabled = true;
            });
            window.clearInterval(timer);
        }
    }

    updateCountdown();
    if (expiration > Date.now()) timer = window.setInterval(updateCountdown, 1000);
})();
