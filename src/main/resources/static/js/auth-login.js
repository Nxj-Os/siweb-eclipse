const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");
const loginSubmit = document.querySelector("#login-submit");

if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        loginMessage.textContent = "Verificando acceso...";
        loginMessage.dataset.state = "pending";
        loginSubmit.disabled = true;

        const formData = new FormData(loginForm);

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify({
                    username: formData.get("username"),
                    password: formData.get("password")
                })
            });
            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.mensaje || result.error || "No se pudo iniciar sesión.");
            }

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