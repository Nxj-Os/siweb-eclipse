(() => {
    const form = document.querySelector(".hold-payment-form");
    const help = document.querySelector("#payment-method-help");
    if (!form || !help) return;

    const radios = [...form.querySelectorAll('input[name="metodoPago"]')];
    const yapeFields = document.querySelector("#yape-fields");
    const cardFields = document.querySelector("#card-fields");
    const cashInstructions = document.querySelector("#cash-instructions");
    const fieldset = form.querySelector(".duration-selection");
    const submit = form.querySelector("button[type='submit']");
    const defaultSubmitLabel = submit.textContent.trim();
    const demo = form.dataset.demo === "true";
    const yapeInputs = {
        phone: document.querySelector("#yape-phone"),
        code: document.querySelector("#yape-approval-code")
    };
    const cardInputs = {
        number: document.querySelector("#card-number"),
        expiry: document.querySelector("#card-expiry"),
        cvc: document.querySelector("#card-cvc"),
        name: document.querySelector("#cardholder-name")
    };

    function validarNumeroTarjeta(valor) {
        const digits = valor.replace(/\D/g, "");
        if (!/^\d{13,19}$/.test(digits)) return false;
        let suma = 0;
        let duplicar = false;
        for (let i = digits.length - 1; i >= 0; i -= 1) {
            let digito = Number(digits[i]);
            if (duplicar) {
                digito *= 2;
                if (digito > 9) digito -= 9;
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 === 0;
    }

    function errorTarjeta(clave) {
        const valor = cardInputs[clave].value.trim();
        if (clave === "number" && !validarNumeroTarjeta(valor)) return "Ingresa un número de tarjeta válido.";
        if (clave === "expiry") {
            const coincidencia = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(valor);
            if (!coincidencia) return "Usa el formato MM/AA.";
            const mes = Number(coincidencia[1]);
            const anio = 2000 + Number(coincidencia[2]);
            const ahora = new Date();
            if (anio < ahora.getFullYear() || (anio === ahora.getFullYear() && mes < ahora.getMonth() + 1)) {
                return "La tarjeta está vencida.";
            }
        }
        if (clave === "cvc" && !/^\d{3,4}$/.test(valor)) return "El CVC debe tener 3 o 4 dígitos.";
        if (clave === "name" && !/^[\p{L}\p{M} .'-]{2,80}$/u.test(valor)) {
            return "Escribe el nombre tal como aparece en la tarjeta.";
        }
        return "";
    }

    function errorYape(clave) {
        const valor = yapeInputs[clave].value.replace(/\D/g, "");
        if (clave === "phone" && !/^9\d{8}$/.test(valor)) return "Ingresa un celular peruano de 9 dígitos que empiece con 9.";
        if (clave === "code" && !/^\d{6}$/.test(valor)) return "Ingresa el código de aprobación de 6 dígitos.";
        return "";
    }

    function validarCampo(tipo, clave) {
        const input = tipo === "card" ? cardInputs[clave] : yapeInputs[clave];
        const error = document.querySelector(`#${input.id}-error`);
        const mensaje = tipo === "card" ? errorTarjeta(clave) : errorYape(clave);
        input.setAttribute("aria-invalid", String(Boolean(mensaje)));
        error.textContent = mensaje;
        error.hidden = !mensaje;
        input.closest(".reserva-field").classList.toggle("is-invalid", Boolean(mensaje));
        return !mensaje;
    }

    function setCamposActivos(inputs, activo) {
        Object.values(inputs).forEach((input) => {
            input.disabled = !activo;
            input.required = activo;
            if (!activo) {
                input.value = "";
                input.removeAttribute("aria-invalid");
                input.closest(".reserva-field").classList.remove("is-invalid");
                document.querySelector(`#${input.id}-error`).hidden = true;
            }
        });
    }

    function actualizarMetodo() {
        const seleccionado = radios.find((radio) => radio.checked)?.value;
        const esEfectivo = seleccionado === "EFECTIVO";
        const esYape = seleccionado === "YAPE";
        const esTarjeta = seleccionado === "TARJETA";
        cashInstructions.hidden = !esEfectivo;
        yapeFields.hidden = !esYape;
        cardFields.hidden = !esTarjeta;
        setCamposActivos(yapeInputs, esYape);
        setCamposActivos(cardInputs, esTarjeta);
        fieldset.classList.remove("is-invalid");
        if (esEfectivo) {
            submit.textContent = submit.dataset.labelCash || "Confirmar reserva";
            help.textContent = "Acércate al área de recepción para realizar el pago.";
            submit.disabled = false;
        } else if (esYape) {
            submit.textContent = defaultSubmitLabel;
            help.textContent = "Completa el celular y el código de aprobación de Yape.";
            submit.disabled = false;
        } else if (esTarjeta && demo) {
            submit.textContent = defaultSubmitLabel;
            help.textContent = "Completa los datos de la tarjeta para continuar.";
            submit.disabled = false;
        } else if (esTarjeta) {
            submit.textContent = defaultSubmitLabel;
            help.textContent = "El pago con tarjeta requiere que el establecimiento configure una pasarela segura.";
            submit.disabled = true;
        } else {
            submit.textContent = defaultSubmitLabel;
            help.textContent = "Selecciona un método de pago.";
            submit.disabled = false;
        }
    }

    yapeInputs.phone.addEventListener("input", () => {
        yapeInputs.phone.value = yapeInputs.phone.value.replace(/\D/g, "").slice(0, 9);
        if (yapeInputs.phone.getAttribute("aria-invalid") === "true") validarCampo("yape", "phone");
    });
    yapeInputs.code.addEventListener("input", () => {
        yapeInputs.code.value = yapeInputs.code.value.replace(/\D/g, "").slice(0, 6);
        if (yapeInputs.code.getAttribute("aria-invalid") === "true") validarCampo("yape", "code");
    });
    Object.entries(yapeInputs).forEach(([clave, input]) => {
        input.addEventListener("blur", () => { if (input.value.trim()) validarCampo("yape", clave); });
    });

    cardInputs.number.addEventListener("input", () => {
        const digits = cardInputs.number.value.replace(/\D/g, "").slice(0, 19);
        cardInputs.number.value = digits.replace(/(.{4})/g, "$1 ").trim();
        if (cardInputs.number.getAttribute("aria-invalid") === "true") validarCampo("card", "number");
    });
    cardInputs.expiry.addEventListener("input", () => {
        const digits = cardInputs.expiry.value.replace(/\D/g, "").slice(0, 4);
        cardInputs.expiry.value = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
        if (cardInputs.expiry.getAttribute("aria-invalid") === "true") validarCampo("card", "expiry");
    });
    cardInputs.cvc.addEventListener("input", () => {
        cardInputs.cvc.value = cardInputs.cvc.value.replace(/\D/g, "").slice(0, 4);
        if (cardInputs.cvc.getAttribute("aria-invalid") === "true") validarCampo("card", "cvc");
    });
    Object.entries(cardInputs).forEach(([clave, input]) => {
        input.addEventListener("input", () => {
            if (input.getAttribute("aria-invalid") === "true") validarCampo("card", clave);
        });
        input.addEventListener("blur", () => { if (input.value.trim()) validarCampo("card", clave); });
    });

    form.addEventListener("invalid", (event) => {
        event.preventDefault();
        if (radios.includes(event.target)) {
            fieldset.classList.add("is-invalid");
            help.textContent = "Selecciona un método de pago para continuar.";
            return;
        }
        const yape = Object.entries(yapeInputs).find(([, input]) => input === event.target);
        const tarjeta = Object.entries(cardInputs).find(([, input]) => input === event.target);
        if (yape) validarCampo("yape", yape[0]);
        if (tarjeta) validarCampo("card", tarjeta[0]);
    }, true);

    form.addEventListener("submit", (event) => {
        const metodo = radios.find((radio) => radio.checked)?.value;
        const inputs = metodo === "YAPE" ? ["yape", yapeInputs] : metodo === "TARJETA" ? ["card", cardInputs] : null;
        if (!inputs) return;
        const [tipo, campos] = inputs;
        const valido = Object.keys(campos).map((clave) => validarCampo(tipo, clave)).every(Boolean);
        if (!valido) {
            event.preventDefault();
            help.textContent = "Revisa los campos marcados antes de continuar.";
            document.querySelector("[aria-invalid='true']")?.focus();
            return;
        }
        if (metodo !== "TARJETA") return;
        event.preventDefault();
        if (!demo) return;
        Object.values(cardInputs).forEach((input) => { input.value = ""; });
        submit.disabled = true;
        form.submit();
    });

    radios.forEach((radio) => radio.addEventListener("change", actualizarMetodo));
    actualizarMetodo();
})();
