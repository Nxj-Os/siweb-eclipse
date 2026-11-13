(() => {
    const form = document.querySelector("#checkin-form");
    const startButton = document.querySelector("#start-qr-scan");
    const video = document.querySelector("#qr-camera");
    const tokenInput = document.querySelector("#token-qr");
    const status = document.querySelector("#qr-scan-status");
    if (!form || !startButton || !video || !tokenInput || !status) return;

    let stream;
    let scanning = false;

    function stopCamera() {
        scanning = false;
        if (stream) {
            stream.getTracks().forEach((track) => track.stop());
            stream = undefined;
        }
        video.srcObject = null;
        video.hidden = true;
        startButton.disabled = false;
    }

    async function scanFrame(detector) {
        if (!scanning) return;
        try {
            const codes = await detector.detect(video);
            if (codes.length > 0 && codes[0].rawValue) {
                tokenInput.value = codes[0].rawValue.trim();
                status.textContent = "Código leído. Validando check-in…";
                stopCamera();
                form.requestSubmit();
                return;
            }
        } catch (error) {
            status.textContent = "No se pudo leer el QR. Escribe el token manualmente.";
            console.error("Error al leer el QR de check-in:", error);
            stopCamera();
            return;
        }
        window.requestAnimationFrame(() => scanFrame(detector));
    }

    startButton.addEventListener("click", async () => {
        if (!("BarcodeDetector" in window) || !navigator.mediaDevices?.getUserMedia) {
            status.textContent = "Este navegador no permite escanear QR con cámara. Escribe el token manualmente.";
            return;
        }
        try {
            const formats = await window.BarcodeDetector.getSupportedFormats();
            if (!formats.includes("qr_code")) {
                status.textContent = "El lector QR no está disponible en este navegador. Escribe el token manualmente.";
                return;
            }
            const detector = new window.BarcodeDetector({ formats: ["qr_code"] });
            stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: { ideal: "environment" } },
                audio: false
            });
            video.srcObject = stream;
            video.hidden = false;
            startButton.disabled = true;
            status.textContent = "Apunta la cámara al código QR.";
            await video.play();
            scanning = true;
            scanFrame(detector);
        } catch (error) {
            status.textContent = "No se pudo abrir la cámara. Revisa los permisos o escribe el token manualmente.";
            console.error("Error al iniciar el escáner QR:", error);
            stopCamera();
        }
    });

    window.addEventListener("pagehide", stopCamera);
})();
