(function () {
    const searchInput = document.querySelector('#buscar-sala');
    const cards = Array.from(document.querySelectorAll('.suite-card'));

    function todayAsISO() {
        const now = new Date();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return now.getFullYear() + '-' + month + '-' + day;
    }

    cards.forEach(function (card) {
        const priceLabel = card.querySelector('[data-suite-price]');
        const today = todayAsISO();
        const validPrices = Array.from(card.querySelectorAll('.suite-card__rates [data-price]'))
            .filter(function (rate) {
                return today >= rate.dataset.from && today <= rate.dataset.until;
            })
            .map(function (rate) {
                return Number(rate.dataset.price);
            });

        if (validPrices.length === 0) {
            const reference = window.suitePrices.referenceStartingPrice(card.dataset.title || '');
            priceLabel.textContent = reference === null
                ? 'Tarifa no disponible'
                : window.suitePrices.formatMoney(reference) + ' · precio referencial del prototipo';
        } else {
            priceLabel.textContent = window.suitePrices.formatMoney(Math.min.apply(null, validPrices));
        }

        const image = card.querySelector('.suite-card__media img');
        image.addEventListener('error', function () {
            if (!image.dataset.fallbackApplied && image.dataset.fallback) {
                image.dataset.fallbackApplied = 'true';
                image.src = image.dataset.fallback;
            }
        });
    });

    if (!searchInput) {
        return;
    }

    searchInput.addEventListener('input', function () {
        const term = searchInput.value.trim().toLowerCase();

        cards.forEach(function (card) {
            if (!term) {
                card.style.display = 'grid';
                return;
            }

            const title = (card.dataset.title || '').toLowerCase();
            const description = (card.dataset.description || '').toLowerCase();
            const amenities = (card.dataset.amenities || '').toLowerCase();
            const matches = title.includes(term) || description.includes(term) || amenities.includes(term);
            card.style.display = matches ? 'grid' : 'none';
        });
    });
})();
