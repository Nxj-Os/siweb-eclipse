const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");
const loginSubmit = document.querySelector("#login-submit");
const registerForm = document.querySelector("#register-form");
const registerMessage = document.querySelector("#register-message");
const registerSubmit = document.querySelector("#register-submit");
let csrfTokenPromise;

function limpiarErrores(form) {
    if (!form) return;
    form.querySelectorAll(".field-error").forEach((el) => el.remove());
    form.querySelectorAll(".input-error").forEach((el) => el.classList.remove("input-error"));
}

function mostrarErroresPorCampo(form, detalles) {
    if (!form || !detalles) return;
    Object.entries(detalles).forEach(([campo, mensaje]) => {
        const input = form.querySelector(`[name="${campo}"]`);
        if (input) {
            input.classList.add("input-error");
            const error = document.createElement("small");
            error.className = "field-error";
            error.textContent = mensaje;
            input.insertAdjacentElement("afterend", error);
        }
    });
}

async function sendJson(url, payload) {
    if (!csrfTokenPromise) {
        csrfTokenPromise = fetch("/api/auth/csrf", {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store"
        }).then(async (response) => {
            if (!response.ok) {
                throw new Error("No se pudo preparar la protección de seguridad. Recarga la página.");
            }
            const csrf = await response.json();
            return csrf.token;
        }).catch((error) => {
            csrfTokenPromise = null;
            throw error;
        });
    }
    const csrfToken = await csrfTokenPromise;
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken
        },
        credentials: "same-origin",
        body: JSON.stringify(payload)
    });
    let result = {};
    try {
        result = await response.json();
    } catch (e) {
        result = {};
    }

    if (!response.ok) {
        const err = new Error(result.mensaje || result.error || "No se pudo completar la solicitud.");
        err.detalles = result.detalles || null;
        err.status = response.status;
        err.code = result.error || "";
        throw err;
    }
    return result;
}

function redirigirTrasLogin(rol) {
    if (rol === "CLIENTE") {
        window.location.href = "/inicio";
        return;
    }
    if (rol === "ADMIN") {
        window.location.href = "/inicio";
        return;
    }
    if (rol === "RECEPCION") {
        window.location.href = "/inicio";
        return;
    }
    if (rol === "ROOMSERVICE") {
        window.location.href = "/inicio";
        return;
    }
    window.location.href = "/inicio";
}

if (loginForm) {
    loginForm.querySelectorAll("[data-toggle-password]").forEach((button) => {
        button.addEventListener("click", () => {
            const input = loginForm.elements[button.dataset.togglePassword];
            const mostrar = input.type === "password";
            input.type = mostrar ? "text" : "password";
            button.classList.toggle("is-visible", mostrar);
            button.setAttribute("aria-pressed", String(mostrar));
            button.setAttribute("aria-label", `${mostrar ? "Ocultar" : "Mostrar"} contraseña`);
        });
    });

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        limpiarErrores(loginForm);
        loginMessage.textContent = "";
        loginMessage.dataset.state = "";

        const formData = new FormData(loginForm);
        const esCliente = document.body.dataset.tipo === "cliente";
        const identity = String(formData.get("identity") || "").trim();
        const password = String(formData.get("password") || "");

        if (esCliente) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!identity) {
                loginMessage.textContent = "Ingresa tu correo electrónico.";
                loginMessage.dataset.state = "error";
                return;
            }
            if (!emailRegex.test(identity)) {
                loginMessage.textContent = "Ingresa un correo electrónico válido.";
                loginMessage.dataset.state = "error";
                return;
            }
        } else {
            if (!identity) {
                loginMessage.textContent = "Ingresa tu usuario.";
                loginMessage.dataset.state = "error";
                return;
            }
            if (identity.length < 3) {
                loginMessage.textContent = "El usuario debe tener al menos 3 caracteres.";
                loginMessage.dataset.state = "error";
                return;
            }
        }

        if (!password) {
            loginMessage.textContent = "Ingresa tu contraseña.";
            loginMessage.dataset.state = "error";
            return;
        }
        if (password.length < 8) {
            loginMessage.textContent = "La contraseña debe tener al menos 8 caracteres.";
            loginMessage.dataset.state = "error";
            return;
        }

        loginMessage.textContent = "Verificando acceso...";
        loginMessage.dataset.state = "pending";
        loginSubmit.disabled = true;
        loginSubmit.setAttribute("aria-busy", "true");
        loginSubmit.classList.add("is-loading");

        const payload = {
            password: password,
            ...(esCliente
                ? { correoElectronico: identity }
                : { username: identity })
        };

        try {
            const endpoint = esCliente ? "/api/auth/client/login" : "/api/auth/login";
            const result = await sendJson(endpoint, payload);

            loginMessage.textContent = "Redirigiendo...";
            loginMessage.dataset.state = "success";
            loginForm.reset();
            redirigirTrasLogin(result.rol);
        } catch (error) {
            if (error.detalles) {
                mostrarErroresPorCampo(loginForm, error.detalles);
                loginMessage.textContent = "Corrige los campos marcados.";
                loginMessage.dataset.state = "error";
            } else {
                loginMessage.textContent = error.message || "No fue posible conectar con el servidor.";
                loginMessage.dataset.state = "error";
            }
        } finally {
            loginSubmit.disabled = false;
            loginSubmit.setAttribute("aria-busy", "false");
            loginSubmit.classList.remove("is-loading");
        }
    });
}

if (registerForm) {
    const domainsAllowed = new Set(
        String(document.body.dataset.dominiosPermitidos || "gmail.com,hotmail.com,outlook.com,yahoo.com")
            .split(",")
            .map((domain) => domain.trim().toLowerCase())
            .filter(Boolean)
    );
    const disposableDomains = new Set([
        "10minutemail.com",
        "dispostable.com",
        "guerrillamail.com",
        "mailinator.com",
        "tempmail.com",
        "yopmail.com"
    ]);
    const commonPasswords = new Set([
        "123456", "12345678", "123456789", "1234567890", "abc123", "admin",
        "admin123", "changeme", "iloveyou", "letmein", "password", "password1",
        "qwerty", "qwerty123", "welcome", "welcome1", "zaq12wsx"
    ]);
    const reglasTelefono = {
        AR: { longitudes: [10, 11, 12, 13] },
        BO: { longitudes: [8] },
        BR: { longitudes: [10, 11] },
        CA: { longitudes: [10] },
        CL: { longitudes: [9] },
        CO: { longitudes: [10] },
        CR: { longitudes: [8] },
        CU: { longitudes: [8] },
        DO: { longitudes: [10] },
        EC: { longitudes: [8, 9] },
        ES: { longitudes: [9] },
        GT: { longitudes: [8] },
        HN: { longitudes: [8] },
        MX: { longitudes: [10] },
        NI: { longitudes: [8] },
        PA: { longitudes: [8] },
        PE: { longitudes: [9], iniciaCon: "9" },
        PY: { longitudes: [9] },
        SV: { longitudes: [8] },
        US: { longitudes: [10] },
        UY: { longitudes: [8] },
        VE: { longitudes: [10] }
    };
    const pendingChecks = new Map();
    const availabilitySequence = new Map();
    const availabilityState = new Map();
    const steps = [...registerForm.querySelectorAll("[data-step]")];
    steps.forEach((step) => {
        step.tabIndex = -1;
    });
    const progressItems = [...document.querySelectorAll("[data-registration-step]")];
    const progressBar = document.querySelector("#registration-progress-bar");
    const progressTrack = document.querySelector(".registration-progress__track");
    const stepLabel = document.querySelector("#registration-step-label");
    const nextButton = document.querySelector("#register-next");
    const backButton = document.querySelector("#register-back");
    let currentStep = 1;
    let continueAfterAvailability = false;

    function fieldFeedback(input) {
        return document.getElementById(`${input.id}-feedback`);
    }

    function setFieldState(input, state, message) {
        const feedback = fieldFeedback(input);
        if (!feedback) return;
        input.classList.toggle("input-error", state === "error");
        input.classList.toggle("input-success", state === "success");
        input.setAttribute("aria-invalid", state === "error" ? "true" : "false");
        feedback.textContent = message;
        feedback.dataset.state = state;
    }

    function phoneRegion() {
        return registerForm.querySelector("#telefonoPrefijo").value;
    }

    function nombrePaisSeleccionado() {
        const option = registerForm.querySelector("#telefonoPrefijo").selectedOptions[0];
        return option.textContent.replace(/^\+\d+\s*/, "").trim();
    }

    function listarDigitos(longitudes) {
        if (longitudes.length === 1) return String(longitudes[0]);
        if (longitudes.length === 2) return `${longitudes[0]} o ${longitudes[1]}`;
        return `${longitudes.slice(0, -1).join(", ")} o ${longitudes[longitudes.length - 1]}`;
    }

    function analizarTelefono() {
        const input = registerForm.elements.telefono;
        const option = registerForm.querySelector("#telefonoPrefijo").selectedOptions[0];
        const region = option.value;
        const prefijo = option.dataset.prefijo;
        const codigo = prefijo.replace(/^\+/, "");
        const regla = reglasTelefono[region];
        const empezoConMas = input.value.trim().startsWith("+");
        let nacional = input.value.replace(/\D/g, "");

        if (empezoConMas) {
            return {
                region,
                prefijo,
                regla,
                empezoConMas,
                codigoCoincide: nacional.startsWith(codigo),
                e164: `+${nacional}`,
                nacional: nacional.startsWith(codigo) ? nacional.slice(codigo.length) : nacional
            };
        }

        if (regla && nacional.startsWith(codigo) && nacional.length > codigo.length) {
            const resto = nacional.slice(codigo.length);
            if (regla.longitudes.includes(resto.length)) {
                nacional = resto;
            }
        }
        if (regla && nacional.startsWith("0") && regla.longitudes.includes(nacional.length - 1)) {
            nacional = nacional.slice(1);
        }

        return {
            region,
            prefijo,
            regla,
            empezoConMas,
            codigoCoincide: true,
            e164: `${prefijo}${nacional}`,
            nacional
        };
    }

    function normalizedPhone() {
        return analizarTelefono().e164;
    }

    function evaluarTelefono() {
        const analisis = analizarTelefono();
        const regla = analisis.regla;
        const digitos = analisis.nacional;

        if (!digitos) {
            return { ...analisis, valido: false, incompleto: true,
                motivo: "El teléfono es obligatorio." };
        }

        if (!regla) {
            const valido = /^\+[1-9]\d{7,14}$/.test(analisis.e164);
            return { ...analisis, valido, incompleto: false,
                motivo: "Ingresa un teléfono válido para el país seleccionado." };
        }

        const nombre = nombrePaisSeleccionado();
        const textoLongitud = `debe tener ${listarDigitos(regla.longitudes)} dígitos`;

        if (analisis.empezoConMas && !analisis.codigoCoincide) {
            return { ...analisis, valido: false, incompleto: false,
                motivo: `El número no corresponde al prefijo de ${nombre}.` };
        }

        const minima = Math.min(...regla.longitudes);
        if (digitos.length < minima) {
            return { ...analisis, valido: false, incompleto: true,
                motivo: `El teléfono de ${nombre} ${textoLongitud}.` };
        }

        if (!regla.longitudes.includes(digitos.length)) {
            return { ...analisis, valido: false, incompleto: false,
                motivo: `El teléfono de ${nombre} ${textoLongitud}.` };
        }

        if (regla.iniciaCon && !digitos.startsWith(regla.iniciaCon)) {
            return { ...analisis, valido: false, incompleto: false,
                motivo: `El teléfono de ${nombre} ${textoLongitud} y debe empezar con ${regla.iniciaCon}.` };
        }

        return { ...analisis, valido: true, incompleto: false, motivo: "" };
    }

    function validatePhone(mostrarError) {
        const input = registerForm.elements.telefono;
        const resultado = evaluarTelefono();
        if (resultado.valido) {
            setFieldState(input, "", "");
            return true;
        }
        if (resultado.incompleto && mostrarError !== true) {
            setFieldState(input, "", "");
            return false;
        }
        setFieldState(input, "error", resultado.motivo);
        return false;
    }

    function aplicarMaximoTelefono() {
        const regla = reglasTelefono[phoneRegion()];
        const input = registerForm.elements.telefono;
        input.maxLength = regla ? Math.max(...regla.longitudes) + 8 : 16;
    }

    function validateEmail() {
        const input = registerForm.elements.correoElectronico;
        const value = input.value.trim().toLowerCase();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            setFieldState(input, "error", "Ingresa un correo electrónico válido.");
            return false;
        }
        const domain = value.substring(value.lastIndexOf("@") + 1);
        if (disposableDomains.has(domain)) {
            setFieldState(input, "error", "No se aceptan correos temporales o desechables.");
            return false;
        }
        if (!domainsAllowed.has(domain)) {
            setFieldState(input, "error",
                `Usa uno de estos dominios permitidos: ${[...domainsAllowed].join(", ")}.`);
            return false;
        }
        input.value = value;
        setFieldState(input, "", "");
        return true;
    }

    function validateEmailConfirmation() {
        const input = registerForm.elements.confirmCorreoElectronico;
        if (!input.value.trim()) {
            setFieldState(input, "", "");
            return false;
        }
        const email = registerForm.elements.correoElectronico.value.trim().toLowerCase();
        const confirmation = input.value.trim().toLowerCase();
        const matches = email !== "" && confirmation === email;
        setFieldState(input, matches ? "success" : "error",
            matches ? "Los correos coinciden." : "Los correos no coinciden.");
        return matches;
    }

    function validateDocument() {
        const type = registerForm.elements.tipoDocumento.value;
        const input = registerForm.elements.numDocumento;
        const value = input.value.trim().toUpperCase();
        const valid = type === "DNI"
            ? /^\d{8}$/.test(value)
            : /^[A-Z0-9]{6,20}$/.test(value);
        if (!valid) {
            setFieldState(input, "error", type === "DNI"
                ? "El DNI debe tener exactamente 8 dígitos."
                : "Usa entre 6 y 20 letras o números.");
            return false;
        }
        input.value = value;
        setFieldState(input, "", "");
        return true;
    }

    function validateAge() {
        const input = registerForm.elements.edad;
        const age = Number(input.value);
        const valid = input.value !== "" && Number.isInteger(age) && age >= 18 && age <= 120;
        setFieldState(input, valid ? "success" : "error",
            valid ? "Edad válida." : "Debes ser mayor de 18 años.");
        return valid;
    }

    function validatePrivacyConsent() {
        const aceptaTerminos = registerForm.elements.aceptaTerminos;
        const aceptaPrivacidad = registerForm.elements.aceptaPoliticaPrivacidad;
        const terminosOk = aceptaTerminos.checked;
        const privacidadOk = aceptaPrivacidad.checked;

        setFieldState(aceptaTerminos, terminosOk ? "success" : "error",
            terminosOk ? "Aceptación de Términos registrada."
                : "Debes aceptar los Términos y Condiciones.");
        setFieldState(aceptaPrivacidad, privacidadOk ? "success" : "error",
            privacidadOk ? "Aceptación de la Política registrada."
                : "Debes aceptar la Política de Datos Personales.");
        return terminosOk && privacidadOk;
    }

    function normalizePasswordText(value) {
        return String(value || "").normalize("NFKC").toLowerCase();
    }

    function passwordHasPersonalData(password) {
        const personalValues = [
            registerForm.elements.nombre.value,
            registerForm.elements.apellido.value,
            registerForm.elements.correoElectronico.value.split("@")[0],
            registerForm.elements.numDocumento.value
        ];
        return personalValues.some((value) => {
            const normalized = normalizePasswordText(value).replace(/[^\p{L}\p{N}]/gu, "");
            return Array.from(normalized).length >= 3
                && normalizePasswordText(password).includes(normalized);
        });
    }

    function validatePassword() {
        const input = registerForm.elements.password;
        const password = input.value;
        const characters = Array.from(password).length;
        const bytes = new TextEncoder().encode(password).length;
        const normalized = normalizePasswordText(password);
        const personalData = passwordHasPersonalData(password);
        const prohibited = commonPasswords.has(normalized);
        const valid = characters >= 8 && characters <= 20 && bytes <= 72
            && !prohibited && !personalData;

        let strength = "very-weak";
        let label = "Muy débil";
        if (valid && characters >= 18) {
            strength = "very-strong";
            label = "Muy fuerte";
        } else if (valid && characters >= 15) {
            strength = "strong";
            label = "Fuerte";
        } else if (valid && characters >= 12) {
            strength = "acceptable";
            label = "Aceptable";
        } else if (valid) {
            strength = "weak";
            label = "Débil";
        }

        document.getElementById("password-strength-label").textContent = label;
        const strengthBar = document.getElementById("password-strength-bar");
        strengthBar.dataset.strength = password ? strength : "empty";
        strengthBar.style.width = password
            ? ({ "very-weak": "12%", weak: "30%", acceptable: "55%", strong: "78%", "very-strong": "100%" }[strength])
            : "0%";

        const feedback = fieldFeedback(input);
        if (!password) {
            setFieldState(input, "", "");
        } else if (prohibited) {
            setFieldState(input, "error", "Esta contraseña es común; elige una frase más larga y única para ti.");
        } else if (personalData) {
            setFieldState(input, "error", "No incluyas tu nombre, apellido, correo o documento.");
        } else if (characters < 8 || characters > 20 || bytes > 72) {
            setFieldState(input, "error", "Usa entre 8 y 20 caracteres.");
        } else {
            setFieldState(input, "success", "Longitud válida.");
        }
        return valid;
    }

    function validatePasswordConfirmation() {
        const input = registerForm.elements.confirmPassword;
        const matches = input.value.length > 0 && input.value === registerForm.elements.password.value;
        if (!input.value) {
            setFieldState(input, "", "");
            return false;
        }
        setFieldState(input, matches ? "success" : "error",
            matches ? "Las contraseñas coinciden." : "Las contraseñas no coinciden.");
        return matches;
    }

    function validateName(input, label) {
        const value = input.value.trim();
        if (value.length < 2 || !/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/.test(value)) {
            setFieldState(input, "error", `${label} debe tener al menos 2 letras.`);
            return false;
        }
        input.value = value;
        setFieldState(input, "", "");
        return true;
    }

    function validateAvailability(input, field, value, type, region) {
        clearTimeout(pendingChecks.get(field));
        if (!value) {
            availabilityState.set(field, "invalid");
            refreshCreateButton();
            return;
        }
        const sequence = (availabilitySequence.get(field) || 0) + 1;
        availabilitySequence.set(field, sequence);
        availabilityState.set(field, "pending");
        setFieldState(input, "pending",
            field === "telefono" ? "Verificando el teléfono..." : "");
        refreshCreateButton();
        pendingChecks.set(field, setTimeout(async () => {
            const params = new URLSearchParams({ campo: field, valor: value });
            if (type) params.set("tipoDocumento", type);
            if (region) params.set("paisTelefono", region);
            try {
                const response = await fetch(`/api/auth/client/disponibilidad?${params}`, {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store"
                });
                const result = await response.json();
                if (availabilitySequence.get(field) !== sequence) return;
                availabilityState.set(field, response.ok && result.disponible ? "available" : "unavailable");
                setFieldState(input, result.disponible ? "success" : "error", result.mensaje);
            } catch {
                if (availabilitySequence.get(field) !== sequence) return;
                availabilityState.set(field, "error");
                setFieldState(input, "error", "No se pudo comprobar la disponibilidad. Inténtalo de nuevo.");
            } finally {
                if (availabilitySequence.get(field) === sequence) {
                    refreshCreateButton();
                    avanzarSiDisponibilidadConfirmada();
                }
            }
        }, 500));
    }

    function avanzarSiDisponibilidadConfirmada() {
        if (!continueAfterAvailability
                || ["documento", "telefono"].some((field) => availabilityState.get(field) === "pending")) {
            return;
        }

        continueAfterAvailability = false;
        if (["documento", "telefono"].every(
            (field) => availabilityState.get(field) === "available")
                && validarPasoPersonal()) {
            registerMessage.textContent = "";
            cambiarPaso(2);
            return;
        }

        registerMessage.textContent = "No se pudo validar la disponibilidad. Revisa los campos marcados.";
        registerMessage.dataset.state = "error";
    }

    function cambiarPaso(step) {
        currentStep = step;
        steps.forEach((panel) => {
            const active = Number(panel.dataset.step) === step;
            panel.hidden = !active;
            panel.setAttribute("aria-hidden", String(!active));
        });
        progressItems.forEach((item) => {
            if (Number(item.dataset.registrationStep) === step) {
                item.setAttribute("aria-current", "step");
            } else {
                item.removeAttribute("aria-current");
            }
        });
        progressTrack.setAttribute("aria-valuenow", String(step));
        progressBar.style.width = `${((step - 1) / (steps.length - 1)) * 100}%`;
        stepLabel.textContent = [
            "",
            "Paso 1 de 2: Datos personales",
            "Paso 2 de 2: Correo y contraseña"
        ][step];
        backButton.hidden = step === 1;
        nextButton.hidden = step !== 1;
        registerSubmit.hidden = step !== 2;
        steps[step - 1].focus({ preventScroll: true });
        const scrollBehavior = window.matchMedia("(prefers-reduced-motion: reduce)").matches
            ? "auto"
            : "smooth";
        registerForm.closest(".login-layout").scrollIntoView({
            behavior: scrollBehavior,
            block: "start"
        });
    }

    function validarPasoPersonal() {
        const nombreValido = validateName(registerForm.elements.nombre, "El nombre");
        const apellidoValido = validateName(registerForm.elements.apellido, "El apellido");
        const documentoValido = validateDocument();
        const telefonoValido = validatePhone(true);
        const edadValida = validateAge();
        const privacidadValida = validatePrivacyConsent();
        return nombreValido && apellidoValido && documentoValido && telefonoValido
            && edadValida && privacidadValida;
    }

    function validarPasoCredenciales() {
        const correoValido = validateEmail();
        const confirmacionCorreoValida = validateEmailConfirmation();
        const passwordValida = validatePassword();
        const confirmacionValida = validatePasswordConfirmation();
        return correoValido && confirmacionCorreoValida && passwordValida && confirmacionValida;
    }

    function refreshCreateButton() {
        if (!registerSubmit) return;
        const fields = registerForm.elements;
        const personalValido = /^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]{2,100}$/.test(fields.nombre.value.trim())
            && /^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]{2,100}$/.test(fields.apellido.value.trim())
            && (fields.tipoDocumento.value === "DNI"
                ? /^\d{8}$/.test(fields.numDocumento.value.trim())
                : /^[A-Z0-9]{6,20}$/i.test(fields.numDocumento.value.trim()))
            && evaluarTelefono().valido
            && Number.isInteger(Number(fields.edad.value))
            && Number(fields.edad.value) >= 18 && Number(fields.edad.value) <= 120
            && fields.aceptaTerminos.checked
            && fields.aceptaPoliticaPrivacidad.checked
            && availabilityState.get("documento") === "available"
            && availabilityState.get("telefono") === "available";
        const email = fields.correoElectronico.value.trim().toLowerCase();
        const emailConfirmation = fields.confirmCorreoElectronico.value.trim().toLowerCase();
        const emailDomain = email.substring(email.lastIndexOf("@") + 1);
        const password = fields.password.value;
        const passwordLength = Array.from(password).length;
        const passwordValid = passwordLength >= 8 && passwordLength <= 20
            && new TextEncoder().encode(password).length <= 72
            && !commonPasswords.has(normalizePasswordText(password))
            && !passwordHasPersonalData(password);
        const credentialsValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
            && domainsAllowed.has(emailDomain) && !disposableDomains.has(emailDomain)
            && availabilityState.get("correo") === "available"
            && emailConfirmation === email
            && passwordValid && password === fields.confirmPassword.value;
        registerSubmit.disabled = !(personalValido && credentialsValid);
    }

    function actualizarEstadoDisponible(field) {
        continueAfterAvailability = false;
        availabilitySequence.set(field, (availabilitySequence.get(field) || 0) + 1);
        availabilityState.delete(field);
        refreshCreateButton();
    }

    registerForm.elements.correoElectronico.addEventListener("input", () => {
        clearTimeout(pendingChecks.get("correo"));
        actualizarEstadoDisponible("correo");
        validateEmail();
        if (registerForm.elements.confirmCorreoElectronico.value) validateEmailConfirmation();
        if (registerForm.elements.password.value) validatePassword();
        refreshCreateButton();
    });
    registerForm.elements.correoElectronico.addEventListener("blur", () => {
        if (validateEmail()) {
            validateAvailability(registerForm.elements.correoElectronico, "correo",
                registerForm.elements.correoElectronico.value.trim().toLowerCase());
        }
    });
    registerForm.elements.confirmCorreoElectronico.addEventListener("input", () => {
        validateEmailConfirmation();
        refreshCreateButton();
    });
    registerForm.elements.confirmCorreoElectronico.addEventListener("blur", validateEmailConfirmation);
    registerForm.elements.numDocumento.addEventListener("input", () => {
        clearTimeout(pendingChecks.get("documento"));
        actualizarEstadoDisponible("documento");
        validateDocument();
        if (registerForm.elements.password.value) validatePassword();
    });
    registerForm.elements.numDocumento.addEventListener("blur", () => {
        if (validateDocument()) {
            validateAvailability(registerForm.elements.numDocumento, "documento",
                registerForm.elements.numDocumento.value.trim().toUpperCase(),
                registerForm.elements.tipoDocumento.value);
        }
    });
    registerForm.elements.tipoDocumento.addEventListener("change", () => {
        clearTimeout(pendingChecks.get("documento"));
        actualizarEstadoDisponible("documento");
        validateDocument();
    });
    registerForm.elements.telefono.addEventListener("input", () => {
        clearTimeout(pendingChecks.get("telefono"));
        actualizarEstadoDisponible("telefono");
        if (validatePhone()) {
            validateAvailability(registerForm.elements.telefono, "telefono", normalizedPhone(),
                null, phoneRegion());
        }
    });
    registerForm.elements.telefono.addEventListener("blur", () => {
        if (validatePhone(true) && availabilityState.get("telefono") !== "available"
                && availabilityState.get("telefono") !== "pending") {
            validateAvailability(registerForm.elements.telefono, "telefono", normalizedPhone(),
                null, phoneRegion());
        }
    });
    registerForm.querySelector("#telefonoPrefijo").addEventListener("change", () => {
        aplicarMaximoTelefono();
        clearTimeout(pendingChecks.get("telefono"));
        actualizarEstadoDisponible("telefono");
        if (validatePhone(true)) {
            validateAvailability(registerForm.elements.telefono, "telefono", normalizedPhone(),
                null, phoneRegion());
        }
    });
    registerForm.elements.nombre.addEventListener("input", () => {
        validateName(registerForm.elements.nombre, "El nombre");
        if (registerForm.elements.password.value) validatePassword();
    });
    registerForm.elements.apellido.addEventListener("input", () => validateName(registerForm.elements.apellido, "El apellido"));
    registerForm.elements.edad.addEventListener("input", validateAge);
    registerForm.elements.aceptaTerminos.addEventListener("change", validatePrivacyConsent);
    registerForm.elements.aceptaPoliticaPrivacidad.addEventListener("change", validatePrivacyConsent);
    registerForm.elements.password.addEventListener("input", () => {
        validatePassword();
        validatePasswordConfirmation();
        refreshCreateButton();
    });
    registerForm.elements.confirmPassword.addEventListener("input", () => {
        validatePasswordConfirmation();
        refreshCreateButton();
    });
    registerForm.addEventListener("input", refreshCreateButton);
    registerForm.addEventListener("change", refreshCreateButton);
    registerForm.querySelectorAll("[data-toggle-password]").forEach((button) => {
        button.addEventListener("click", () => {
            const input = registerForm.elements[button.dataset.togglePassword];
            const mostrar = input.type === "password";
            input.type = mostrar ? "text" : "password";
            button.classList.toggle("is-visible", mostrar);
            button.setAttribute("aria-pressed", String(mostrar));
            button.setAttribute("aria-label", `${mostrar ? "Ocultar" : "Mostrar"} contraseña`);
        });
    });

    nextButton.addEventListener("click", () => {
        limpiarErrores(registerForm);
        if (!validarPasoPersonal()) {
            registerMessage.textContent = "Revisa los datos personales y corrige el primer campo marcado.";
            registerMessage.dataset.state = "error";
            registerForm.querySelector('[data-step="1"] .input-error')?.focus();
            return;
        }
        const datosDisponibles = ["documento", "telefono"].every(
            (field) => availabilityState.get(field) === "available");
        if (!datosDisponibles) {
            continueAfterAvailability = true;
            if (availabilityState.get("documento") !== "available") {
                validateAvailability(registerForm.elements.numDocumento, "documento",
                    registerForm.elements.numDocumento.value.trim().toUpperCase(),
                    registerForm.elements.tipoDocumento.value);
            }
            if (availabilityState.get("telefono") !== "available") {
                validateAvailability(registerForm.elements.telefono, "telefono", normalizedPhone(),
                    null, phoneRegion());
            }
            registerMessage.textContent = "Comprobando que el documento y teléfono estén disponibles. Continúa cuando termine la consulta.";
            registerMessage.dataset.state = "pending";
            return;
        }
        registerMessage.textContent = "";
        cambiarPaso(2);
    });

    backButton.addEventListener("click", () => {
        registerMessage.textContent = "";
        cambiarPaso(1);
    });

    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (currentStep !== 2) return;
        limpiarErrores(registerForm);
        registerMessage.textContent = "";
        if (!validarPasoPersonal() || !validarPasoCredenciales()) {
            registerMessage.textContent = "Revisa los campos marcados y corrige el primer error.";
            registerMessage.dataset.state = "error";
            registerForm.querySelector(".input-error")?.focus();
            return;
        }
        if (registerSubmit.disabled) return;

        const formData = new FormData(registerForm);
        const correo = String(formData.get("correoElectronico") || "").trim().toLowerCase();
        registerMessage.textContent = "Creando tu cuenta...";
        registerMessage.dataset.state = "pending";
        registerSubmit.disabled = true;
        registerSubmit.setAttribute("aria-busy", "true");
        backButton.disabled = true;
        formData.set("telefono", normalizedPhone());
        formData.set("paisTelefono", phoneRegion());
        formData.delete("confirmPassword");
        formData.delete("confirmCorreoElectronico");
        formData.delete("aceptaTerminos");
        formData.set("nombre", String(formData.get("nombre")).trim());
        formData.set("apellido", String(formData.get("apellido")).trim());
        formData.set("numDocumento", String(formData.get("numDocumento")).trim().toUpperCase());
        formData.set("correoElectronico", correo);

        try {
            const payload = Object.fromEntries(formData);
            payload.aceptaPoliticaPrivacidad = true;
            const result = await sendJson("/api/auth/client/register", payload);
            registerMessage.textContent = "Cuenta creada. Iniciando sesión...";
            registerMessage.dataset.state = "success";
            registerForm.reset();
            redirigirTrasLogin(result.rol);
        } catch (error) {
            if (error.detalles) {
                registerMessage.textContent = "Corrige los campos marcados.";
                registerMessage.dataset.state = "error";
                mostrarErroresPorCampo(registerForm, error.detalles);
                cambiarPaso(2);
            } else {
                registerMessage.textContent = error.message || "No fue posible crear la cuenta.";
                registerMessage.dataset.state = "error";
            }
        } finally {
            backButton.disabled = false;
            registerSubmit.setAttribute("aria-busy", "false");
            refreshCreateButton();
        }
    });

    const termsModal = document.querySelector("#terms-modal");
    const openTermsButton = document.querySelector("#open-terms");
    let termsTrigger = null;

    function elementosEnfocablesDelModal() {
        if (!termsModal) return [];
        return [...termsModal.querySelectorAll(
            "button, [href], input, select, textarea, [tabindex]:not([tabindex='-1'])"
        )].filter((elemento) => !elemento.disabled && elemento.offsetParent !== null);
    }

    function abrirModalTerminos() {
        if (!termsModal) return;
        termsTrigger = document.activeElement;
        termsModal.hidden = false;
        document.body.classList.add("modal-open");
        termsModal.querySelector(".terms-modal__body")?.scrollTo({ top: 0 });
        termsModal.querySelector(".terms-modal__close")?.focus();
    }

    function cerrarModalTerminos() {
        if (!termsModal || termsModal.hidden) return;
        termsModal.hidden = true;
        document.body.classList.remove("modal-open");
        if (termsTrigger && typeof termsTrigger.focus === "function") {
            termsTrigger.focus();
        }
        termsTrigger = null;
    }

    openTermsButton?.addEventListener("click", abrirModalTerminos);
    termsModal?.querySelectorAll("[data-close-terms]").forEach((elemento) => {
        elemento.addEventListener("click", cerrarModalTerminos);
    });
    document.addEventListener("keydown", (event) => {
        if (!termsModal || termsModal.hidden) return;
        if (event.key === "Escape") {
            event.preventDefault();
            cerrarModalTerminos();
            return;
        }
        if (event.key === "Tab") {
            const foco = elementosEnfocablesDelModal();
            if (!foco.length) return;
            const primero = foco[0];
            const ultimo = foco[foco.length - 1];
            if (event.shiftKey && document.activeElement === primero) {
                event.preventDefault();
                ultimo.focus();
            } else if (!event.shiftKey && document.activeElement === ultimo) {
                event.preventDefault();
                primero.focus();
            } else if (!termsModal.contains(document.activeElement)) {
                event.preventDefault();
                primero.focus();
            }
        }
    });

    aplicarMaximoTelefono();
    refreshCreateButton();
}