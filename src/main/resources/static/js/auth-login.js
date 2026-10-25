const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");
const loginSubmit = document.querySelector("#login-submit");
const registerForm = document.querySelector("#register-form");
const registerMessage = document.querySelector("#register-message");
const registerSubmit = document.querySelector("#register-submit");

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
    const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
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
        }
    });
}

if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        limpiarErrores(registerForm);
        registerMessage.textContent = "";
        registerMessage.dataset.state = "";

        const formData = new FormData(registerForm);
        const password = String(formData.get("password") || "");
        const confirmPassword = String(formData.get("confirmPassword") || "");
        const correo = String(formData.get("correoElectronico") || "").trim();
        const nombre = String(formData.get("nombre") || "").trim();
        const apellido = String(formData.get("apellido") || "").trim();
        const numDocumento = String(formData.get("numDocumento") || "").trim();
        const telefono = String(formData.get("telefono") || "").trim();
        const edadStr = String(formData.get("edad") || "").trim();
        const edad = Number(edadStr);

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const soloLetras = /^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/;
        const soloDoc = /^[A-Za-z0-9]+$/;
        const soloTel = /^[0-9+\s()-]+$/;

        if (!nombre || nombre.length < 2 || !soloLetras.test(nombre)) {
            registerMessage.textContent = "El nombre debe tener al menos 2 letras y solo letras.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!apellido || apellido.length < 2 || !soloLetras.test(apellido)) {
            registerMessage.textContent = "El apellido debe tener al menos 2 letras y solo letras.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!numDocumento || numDocumento.length < 8 || !soloDoc.test(numDocumento)) {
            registerMessage.textContent = "El número de documento debe tener al menos 8 caracteres (solo letras y números).";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!telefono || telefono.length < 7 || !soloTel.test(telefono)) {
            registerMessage.textContent = "Ingresa un teléfono válido.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!correo || !emailRegex.test(correo)) {
            registerMessage.textContent = "Ingresa un correo electrónico válido.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!edadStr || Number.isNaN(edad) || edad < 18 || edad > 120) {
            registerMessage.textContent = "Debes ser mayor de 18 años para registrarte.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (!password || password.length < 8) {
            registerMessage.textContent = "La contraseña debe tener al menos 8 caracteres.";
            registerMessage.dataset.state = "error";
            return;
        }
        if (password !== confirmPassword) {
            registerMessage.textContent = "Las contraseñas no coinciden.";
            registerMessage.dataset.state = "error";
            return;
        }

        registerMessage.textContent = "Creando tu cuenta...";
        registerMessage.dataset.state = "pending";
        registerSubmit.disabled = true;
        const phonePrefix = registerForm.querySelector("#telefonoPrefijo").value;
        const phoneNumber = telefono;
        formData.set("telefono", `${phonePrefix} ${phoneNumber}`);
        formData.delete("confirmPassword");
        formData.set("nombre", nombre);
        formData.set("apellido", apellido);
        formData.set("numDocumento", numDocumento);
        formData.set("correoElectronico", correo.toLowerCase());

        try {
            const payload = Object.fromEntries(formData);
            const result = await sendJson("/api/auth/client/register", payload);
            registerMessage.textContent = "Redirigiendo...";
            registerMessage.dataset.state = "success";
            registerForm.reset();
            window.location.href = "/inicio";
        } catch (error) {
            if (error.detalles) {
                mostrarErroresPorCampo(registerForm, error.detalles);
                registerMessage.textContent = "Corrige los campos marcados.";
                registerMessage.dataset.state = "error";
            } else {
                registerMessage.textContent = error.message || "No fue posible crear la cuenta.";
                registerMessage.dataset.state = "error";
            }
        } finally {
            registerSubmit.disabled = false;
        }
    });
}