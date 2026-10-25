(function () {
    const searchInput = document.querySelector('#buscar-sala');
    const cards = Array.from(document.querySelectorAll('.suite-card'));

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
