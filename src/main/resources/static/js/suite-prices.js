(function () {
    const figmaStartingPrices = {
        basica: 50,
        junior: 80,
        panoramica: 120,
        roja: 150,
        jacuzzi: 170,
        'sauna con jacuzzi': 200,
        presidencial: 250
    };

    function normalizeText(value) {
        return String(value || '').toLocaleLowerCase().normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '');
    }

    window.suitePrices = {
        formatMoney: function (amount) {
            return new Intl.NumberFormat('es-PE', {
                style: 'currency',
                currency: 'PEN'
            }).format(Number(amount));
        },
        referenceStartingPrice: function (title) {
            const normalizedTitle = normalizeText(title);
            const match = Object.keys(figmaStartingPrices)
                .sort(function (left, right) {
                    return right.length - left.length;
                })
                .find(function (suiteName) {
                    return normalizedTitle.includes(suiteName);
                });
            return match ? figmaStartingPrices[match] : null;
        }
    };
})();
