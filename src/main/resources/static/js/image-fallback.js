(function () {
    function applyFallback(image) {
        if (!(image instanceof HTMLImageElement) || !image.dataset.fallback || image.dataset.fallbackApplied === "true") {
            return;
        }
        const src = image.getAttribute("src");
        if (!src) {
            return;
        }
        image.dataset.fallbackApplied = "true";
        image.src = image.dataset.fallback;
    }

    document.addEventListener("error", function (event) {
        applyFallback(event.target);
    }, true);

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("img[data-fallback]").forEach(function (img) {
            if (img.complete && img.naturalWidth === 0) {
                applyFallback(img);
            }
        });
    });
})();
