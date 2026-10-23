const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");
const loginSubmit = document.querySelector("#login-submit");
const registerForm = document.querySelector("#register-form");
const registerMessage = document.querySelector("#register-message");
const registerSubmit = document.querySelector("#register-submit");

async function sendJson(url, payload) {
    const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify(payload)
    });
    const result = await response.json();

    if (!response.ok) {
        const validationMessage = result.detalles
            ? Object.values(result.detalles).join(" ")
            : "";
        throw new Error(validationMessage || result.mensaje || result.error || "No se pudo completar la solicitud.");
    }
    return result;
}

if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        loginMessage.textContent = "Verificando acceso...";
        loginMessage.dataset.state = "pending";
        loginSubmit.disabled = true;

        const formData = new FormData(loginForm);
        const esCliente = document.body.dataset.tipo === "cliente";
        const payload = {
            password: formData.get("password"),
            ...(esCliente
                ? { correoElectronico: formData.get("identity") }
                : { username: formData.get("identity") })
        };

        try {
            const endpoint = esCliente ? "/api/auth/client/login" : "/api/auth/login";
            const result = await sendJson(endpoint, payload);

            loginMessage.textContent = `Bienvenido, ${result.nombreCompleto}. ${result.mensaje || "Sesión iniciada."}`;
            loginMessage.dataset.state = "success";
            loginForm.reset();
        } catch (error) {
            loginMessage.textContent = error.message || "No fue posible conectar con el servidor.";
            loginMessage.dataset.state = "error";
        } finally {
            loginSubmit.disabled = false;
        }
    });
}

if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const formData = new FormData(registerForm);
        const password = formData.get("password");

        if (password !== formData.get("confirmPassword")) {
            registerMessage.textContent = "Las contraseñas no coinciden.";
            registerMessage.dataset.state = "error";
            return;
        }

        registerMessage.textContent = "Creando tu cuenta...";
        registerMessage.dataset.state = "pending";
        registerSubmit.disabled = true;
        const phonePrefix = registerForm.querySelector("#telefonoPrefijo").value;
        const phoneNumber = String(formData.get("telefono")).trim();
        formData.set("telefono", `${phonePrefix} ${phoneNumber}`);
        formData.delete("confirmPassword");

        try {
            const result = await sendJson("/api/auth/client/register", Object.fromEntries(formData));
            registerMessage.textContent = `Cuenta creada. Bienvenido, ${result.nombreCompleto}. Tu sesión ya está iniciada.`;
            registerMessage.dataset.state = "success";
            registerForm.reset();
        } catch (error) {
            registerMessage.textContent = error.message || "No fue posible crear la cuenta.";
            registerMessage.dataset.state = "error";
        } finally {
            registerSubmit.disabled = false;
        }
    });
}