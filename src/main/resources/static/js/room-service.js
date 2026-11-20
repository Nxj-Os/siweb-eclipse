/* Carta de room service para el huésped (Figma C-09).
 * Acceso por token de check-in en la URL (?token=...), carrito en el
 * navegador, envío del pedido con CSRF y seguimiento del estado con
 * consultas periódicas (sin WebSocket: las notificaciones del lado
 * empleado las resuelve el sistema de recepción). */

(function () {
    "use strict";

    const ETIQUETAS_ESTADO = {
        RECIBIDO: "Recibido",
        EN_PREPARACION: "En preparación",
        CONTROLADO: "Controlado",
        ENTREGADO: "Entregado"
    };
    const ORDEN_ESTADOS = ["RECIBIDO", "EN_PREPARACION", "CONTROLADO", "ENTREGADO"];
    const INTERVALO_ACTUALIZACION_MS = 10000;
    const EMOJI_CATEGORIA = {
        "Entradas": "🥗",
        "Platos fuertes": "🍛",
        "Bebidas": "🥤",
        "Postres": "🍰",
        "Snacks": "🍟"
    };

    const token = new URLSearchParams(window.location.search).get("token");

    const estado = {
        categorias: [],
        productos: [],
        carrito: new Map(),
        categoriaActiva: null,
        consulta: "",
        codigoPedido: null,
        estadoPedido: null,
        temporizador: null
    };

    // ------------------------------------------------------- helpers

    function porId(id) {
        return document.getElementById(id);
    }

    function crear(tag, clase, texto) {
        const nodo = document.createElement(tag);
        if (clase) {
            nodo.className = clase;
        }
        if (texto !== undefined && texto !== null) {
            nodo.textContent = texto;
        }
        return nodo;
    }

    function formatoSoles(valor) {
        return "S/ " + Number(valor || 0).toFixed(2);
    }

    function mostrarAviso(mensaje, exito) {
        const aviso = porId("aviso-global");
        aviso.textContent = mensaje;
        aviso.classList.toggle("aviso-global--exito", Boolean(exito));
        aviso.hidden = false;
        window.clearTimeout(mostrarAviso.temporizador);
        mostrarAviso.temporizador = window.setTimeout(() => {
            aviso.hidden = true;
        }, 5000);
    }

    let promesaCsrf = null;

    async function obtenerTokenCsrf() {
        if (!promesaCsrf) {
            promesaCsrf = fetch("/api/auth/csrf", {
                headers: { Accept: "application/json" }
            })
                .then(async (respuesta) => {
                    if (!respuesta.ok) {
                        throw new Error("No se pudo obtener el token de seguridad");
                    }
                    const datos = await respuesta.json();
                    return datos.token;
                })
                .catch((error) => {
                    promesaCsrf = null;
                    throw error;
                });
        }
        return promesaCsrf;
    }

    async function apiJson(ruta, opciones) {
        const config = opciones || {};
        const metodo = (config.metodo || "GET").toUpperCase();
        const encabezados = { Accept: "application/json" };

        if (metodo !== "GET" && metodo !== "HEAD") {
            encabezados["X-XSRF-TOKEN"] = await obtenerTokenCsrf();
            encabezados["Content-Type"] = "application/json";
        }

        let respuesta;
        try {
            respuesta = await fetch(ruta, {
                method: metodo,
                headers: encabezados,
                body: config.cuerpo ? JSON.stringify(config.cuerpo) : undefined
            });
        } catch (error) {
            throw new Error("No se pudo conectar con el servicio. Intenta nuevamente.");
        }

        const datos = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) {
            const fallo = new Error(
                datos.mensaje || datos.error || "No se pudo completar la solicitud."
            );
            fallo.status = respuesta.status;
            fallo.detalles = datos.detalles || null;
            throw fallo;
        }
        return datos;
    }

    // ------------------------------------------------------- acceso

    function bloquearServicio(mensaje) {
        window.clearInterval(estado.temporizador);
        porId("vista-carta").hidden = true;
        porId("bloqueada-mensaje").textContent =
            mensaje || "Necesitas validar el check-in de tu reserva para acceder";
        porId("vista-bloqueada").hidden = false;
    }

    // ------------------------------------------------------- carta

    async function cargarCarta() {
        const carta = await apiJson(
            "/api/roomservice/menu?token=" + encodeURIComponent(token)
        );
        estado.categorias = carta.categorias || [];
        estado.productos = carta.productos || [];

        const badge = porId("badge-suite");
        badge.textContent = "Suite " + carta.numHabitacion;
        badge.hidden = false;

        renderCategorias();
        renderProductos();
        porId("vista-carta").hidden = false;
    }

    function renderCategorias() {
        const contenedor = porId("categorias");
        contenedor.replaceChildren();

        const botonTodas = crear("button", "chip", "Todas");
        botonTodas.type = "button";
        botonTodas.classList.toggle("chip--activo", estado.categoriaActiva === null);
        botonTodas.addEventListener("click", () => {
            estado.categoriaActiva = null;
            renderCategorias();
            renderProductos();
        });
        contenedor.appendChild(botonTodas);

        estado.categorias.forEach((categoria) => {
            const boton = crear("button", "chip", categoria.nombre);
            boton.type = "button";
            boton.classList.toggle(
                "chip--activo",
                estado.categoriaActiva === categoria.idCategoria
            );
            boton.addEventListener("click", () => {
                estado.categoriaActiva = categoria.idCategoria;
                renderCategorias();
                renderProductos();
            });
            contenedor.appendChild(boton);
        });
    }

    function productosFiltrados() {
        const consulta = estado.consulta.trim().toLowerCase();
        return estado.productos.filter((producto) => {
            const coincideCategoria =
                estado.categoriaActiva === null ||
                producto.idCategoria === estado.categoriaActiva;
            const texto = (
                producto.nombre +
                " " +
                (producto.descripcion || "")
            ).toLowerCase();
            return coincideCategoria && (!consulta || texto.includes(consulta));
        });
    }

    function emojiDeProducto(producto) {
        if (producto.imagen && /https?:|^\/(img|images)\//.test(producto.imagen)) {
            return null;
        }
        if (producto.imagen) {
            return producto.imagen;
        }
        return EMOJI_CATEGORIA[producto.categoria] || "🍽️";
    }

    function renderProductos() {
        const contenedor = porId("productos");
        contenedor.replaceChildren();
        const filtrados = productosFiltrados();
        porId("productos-vacios").hidden = filtrados.length > 0;

        filtrados.forEach((producto) => {
            contenedor.appendChild(tarjetaProducto(producto));
        });
    }

    function tarjetaProducto(producto) {
        const enCarrito = estado.carrito.get(producto.idProducto);
        const restante = Math.max(0, (producto.stockDisponible || 0) - (enCarrito || 0));
        const agotado = producto.stockDisponible <= 0;

        const tarjeta = crear("article", "producto");

        const imagen = crear("div", "producto__imagen");
        const emoji = emojiDeProducto(producto);
        if (emoji) {
            imagen.textContent = emoji;
        } else {
            const img = document.createElement("img");
            img.src = producto.imagen;
            img.alt = producto.nombre;
            img.loading = "lazy";
            imagen.appendChild(img);
        }
        tarjeta.appendChild(imagen);

        const stock = crear(
            "span",
            "producto__stock",
            agotado ? "Agotado" : "Quedan " + producto.stockDisponible
        );
        if (agotado) {
            stock.classList.add("producto__stock--agotado");
        } else if (producto.stockDisponible <= 3) {
            stock.classList.add("producto__stock--bajo");
        }
        tarjeta.appendChild(stock);

        const cuerpo = crear("div", "producto__cuerpo");
        cuerpo.appendChild(crear("h3", "producto__nombre", producto.nombre));
        if (producto.descripcion) {
            cuerpo.appendChild(
                crear("p", "producto__descripcion", producto.descripcion)
            );
        }

        const pie = crear("div", "producto__pie");
        pie.appendChild(
            crear("span", "producto__precio", formatoSoles(producto.precio))
        );

        if (enCarrito) {
            pie.appendChild(
                crear("span", "producto__cantidad", enCarrito + " en pedido")
            );
        }

        const boton = crear("button", "producto__agregar", "+");
        boton.type = "button";
        boton.setAttribute("aria-label", "Agregar " + producto.nombre + " al pedido");
        boton.disabled = agotado || restante <= 0;
        boton.addEventListener("click", () => agregarAlCarrito(producto));
        pie.appendChild(boton);

        cuerpo.appendChild(pie);
        tarjeta.appendChild(cuerpo);
        return tarjeta;
    }

    // ------------------------------------------------------ carrito

    function agregarAlCarrito(producto) {
        const actual = estado.carrito.get(producto.idProducto) || 0;
        if (actual >= producto.stockDisponible) {
            mostrarAviso(
                "Solo quedan " + producto.stockDisponible + " de " + producto.nombre + "."
            );
            return;
        }
        estado.carrito.set(producto.idProducto, actual + 1);
        renderProductos();
        renderCarrito();
    }

    function cambiarCantidad(idProducto, delta) {
        const producto = estado.productos.find((p) => p.idProducto === idProducto);
        const actual = estado.carrito.get(idProducto) || 0;
        const nueva = actual + delta;

        if (nueva <= 0) {
            estado.carrito.delete(idProducto);
        } else if (producto && nueva > producto.stockDisponible) {
            mostrarAviso(
                "Solo quedan " + producto.stockDisponible + " de " + producto.nombre + "."
            );
            return;
        } else {
            estado.carrito.set(idProducto, nueva);
        }
        renderProductos();
        renderCarrito();
    }

    function montoDelCarrito() {
        let monto = 0;
        estado.carrito.forEach((cantidad, idProducto) => {
            const producto = estado.productos.find((p) => p.idProducto === idProducto);
            if (producto) {
                monto += Number(producto.precio) * cantidad;
            }
        });
        return monto;
    }

    function renderCarrito() {
        const lista = porId("carrito-lista");
        lista.replaceChildren();

        if (estado.carrito.size === 0) {
            const vacio = crear("p", "carrito__vacio");
            vacio.innerHTML = "Tu pedido está vacío.<br>Agrega productos de la carta.";
            lista.appendChild(vacio);
            porId("carrito-totales").hidden = true;
            porId("btn-solicitar").disabled = true;
            return;
        }

        estado.carrito.forEach((cantidad, idProducto) => {
            const producto = estado.productos.find((p) => p.idProducto === idProducto);
            if (!producto) {
                return;
            }

            const fila = crear("div", "carrito-item");
            fila.appendChild(crear("span", "carrito-item__nombre", producto.nombre));
            fila.appendChild(
                crear(
                    "span",
                    "carrito-item__subtotal",
                    formatoSoles(Number(producto.precio) * cantidad)
                )
            );

            const controles = crear("div", "carrito-item__controles");
            const restar = crear("button", "cant-btn", "−");
            restar.type = "button";
            restar.setAttribute("aria-label", "Quitar uno de " + producto.nombre);
            restar.addEventListener("click", () => cambiarCantidad(idProducto, -1));

            const sumar = crear("button", "cant-btn", "+");
            sumar.type = "button";
            sumar.setAttribute("aria-label", "Agregar uno de " + producto.nombre);
            sumar.addEventListener("click", () => cambiarCantidad(idProducto, 1));

            const quitar = crear("button", "carrito-item__quitar", "Quitar");
            quitar.type = "button";
            quitar.addEventListener("click", () => cambiarCantidad(idProducto, -cantidad));

            controles.appendChild(restar);
            controles.appendChild(
                crear("span", "carrito-item__cantidad", String(cantidad))
            );
            controles.appendChild(sumar);
            controles.appendChild(quitar);
            fila.appendChild(controles);

            lista.appendChild(fila);
        });

        porId("carrito-monto").textContent = formatoSoles(montoDelCarrito());
        porId("carrito-totales").hidden = false;
        porId("btn-solicitar").disabled = false;
    }

    // -------------------------------------------------- enviar pedido

    async function enviarPedido() {
        if (estado.carrito.size === 0) {
            return;
        }
        const boton = porId("btn-solicitar");
        boton.disabled = true;
        boton.textContent = "Enviando…";

        const items = [];
        estado.carrito.forEach((cantidad, idProducto) => {
            items.push({ idProducto: idProducto, cantidad: cantidad });
        });

        try {
            const pedido = await apiJson("/api/roomservice/pedidos", {
                metodo: "POST",
                cuerpo: { token: token, items: items }
            });
            estado.codigoPedido = pedido.codigo;
            estado.estadoPedido = pedido.estado;
            estado.carrito.clear();
            renderCarrito();
            renderProductos();
            mostrarEstadoPedido(pedido, true);
            mostrarAviso("Pedido " + pedido.codigo + " enviado. ¡Buen provecho!", true);
        } catch (error) {
            if (error.status === 403) {
                bloquearServicio(error.message);
                return;
            }
            if (error.status === 409 && error.detalles) {
                const idAgotado = estado.productos.find(
                    (p) => p.nombre === error.detalles.producto
                );
                if (idAgotado) {
                    idAgotado.stockDisponible = Number(error.detalles.stockDisponible);
                }
                const enCarrito = [];
                estado.carrito.forEach((cantidad, id) => enCarrito.push(id));
                enCarrito.forEach((id) => {
                    const producto = estado.productos.find((p) => p.idProducto === id);
                    if (producto && (estado.carrito.get(id) || 0) > producto.stockDisponible) {
                        if (producto.stockDisponible <= 0) {
                            estado.carrito.delete(id);
                        } else {
                            estado.carrito.set(id, producto.stockDisponible);
                        }
                    }
                });
                renderProductos();
                renderCarrito();
            }
            mostrarAviso(error.message);
        } finally {
            boton.textContent = "Solicitar pedido";
            boton.disabled = estado.carrito.size === 0;
        }
    }

    // ------------------------------------------- estado del pedido

    function mostrarEstadoPedido(pedido, reiniciar) {
        window.clearInterval(estado.temporizador);
        estado.codigoPedido = pedido.codigo;
        estado.estadoPedido = pedido.estado;

        porId("modo-carrito").hidden = true;
        porId("modo-estado").hidden = false;

        const indiceActual = ORDEN_ESTADOS.indexOf(pedido.estado);
        document.querySelectorAll("#checklist .checklist__paso").forEach((paso) => {
            const indice = ORDEN_ESTADOS.indexOf(paso.dataset.estado);
            paso.classList.toggle("checklist__paso--hecho", indice <= indiceActual);
            paso.classList.toggle(
                "checklist__paso--actual",
                indice === indiceActual && indiceActual < ORDEN_ESTADOS.length - 1
            );
        });

        const listaItems = porId("estado-items");
        listaItems.replaceChildren();
        (pedido.items || []).forEach((item) => {
            listaItems.appendChild(
                crear("li", null, item.nombre + " (" + item.cantidad + ")")
            );
        });

        porId("estado-monto").textContent = formatoSoles(pedido.montoTotal);
        porId("estado-codigo").textContent = pedido.codigo;

        const aviso = porId("estado-aviso");
        if (pedido.estado === "ENTREGADO") {
            aviso.innerHTML =
                '<i class="bi bi-check2-circle" aria-hidden="true"></i> Pedido entregado. ¡Gracias!';
            window.clearInterval(estado.temporizador);
        } else {
            aviso.innerHTML =
                '<i class="bi bi-arrow-repeat" aria-hidden="true"></i> Actualizamos el estado automáticamente.';
        }

        if (reiniciar && pedido.estado !== "ENTREGADO") {
            estado.temporizador = window.setInterval(
                consultarEstadoPedido,
                INTERVALO_ACTUALIZACION_MS
            );
        }
    }

    async function consultarEstadoPedido() {
        if (!estado.codigoPedido) {
            return;
        }
        try {
            const pedido = await apiJson(
                "/api/roomservice/pedidos/" +
                    encodeURIComponent(estado.codigoPedido) +
                    "?token=" +
                    encodeURIComponent(token)
            );
            if (pedido.estado !== estado.estadoPedido) {
                mostrarEstadoPedido(pedido, true);
            }
        } catch (error) {
            if (error.status === 403 || error.status === 404) {
                window.clearInterval(estado.temporizador);
            }
            // Los fallos transitorios se ignoran: reintentará el próximo ciclo.
        }
    }

    function volverALaCarta() {
        window.clearInterval(estado.temporizador);
        estado.codigoPedido = null;
        estado.estadoPedido = null;
        porId("modo-estado").hidden = true;
        porId("modo-carrito").hidden = false;
        renderCarrito();
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    // ------------------------------------------------------ arranque

    function conectarEventos() {
        let temporizadorBusqueda = null;
        porId("busqueda").addEventListener("input", (evento) => {
            window.clearTimeout(temporizadorBusqueda);
            temporizadorBusqueda = window.setTimeout(() => {
                estado.consulta = evento.target.value;
                renderProductos();
            }, 250);
        });

        porId("btn-solicitar").addEventListener("click", enviarPedido);
        porId("btn-otro").addEventListener("click", volverALaCarta);
    }

    async function iniciar() {
        conectarEventos();

        if (!token) {
            bloquearServicio(
                "No se detectó tu código de acceso. Escanea el QR de tu suite para entrar a la carta."
            );
            return;
        }

        try {
            await cargarCarta();
        } catch (error) {
            if (error.status === 403) {
                bloquearServicio(error.message);
            } else {
                bloquearServicio(
                    "No pudimos cargar la carta en este momento. Vuelve a intentarlo en unos minutos."
                );
            }
        }
    }

    document.addEventListener("DOMContentLoaded", iniciar);
})();
