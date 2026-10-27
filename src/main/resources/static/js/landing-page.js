(() => {
    const menu = document.querySelector("[data-floating-menu]");
    const toggle = menu?.querySelector(".floating-menu__toggle");
    const panel = menu?.querySelector(".floating-menu__panel");

    if (!menu || !toggle || !panel) {
        return;
    }

    const setMenuOpen = (isOpen) => {
        toggle.setAttribute("aria-expanded", String(isOpen));
        toggle.setAttribute("aria-label", isOpen ? "Cerrar menú de navegación" : "Abrir menú de navegación");
        panel.hidden = !isOpen;
        document.body.classList.toggle("menu-is-open", isOpen);
    };

    const updateScrollState = () => {
        document.body.classList.toggle("has-scrolled", window.scrollY > 120);
    };

    toggle.addEventListener("click", () => {
        setMenuOpen(toggle.getAttribute("aria-expanded") !== "true");
    });

    panel.addEventListener("click", (event) => {
        if (event.target instanceof Element && event.target.closest("a")) {
            setMenuOpen(false);
        }
    });

    document.addEventListener("click", (event) => {
        if (!menu.contains(event.target)) {
            setMenuOpen(false);
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && toggle.getAttribute("aria-expanded") === "true") {
            setMenuOpen(false);
            toggle.focus();
        }
    });

    window.addEventListener("scroll", updateScrollState, { passive: true });
    updateScrollState();
})();
