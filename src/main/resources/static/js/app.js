(() => {
    const toggle = document.getElementById("menuToggle");
    const sidebar = document.getElementById("sidebar");
    const scrim = document.getElementById("scrim");
    if (toggle && sidebar && scrim) {
        const close = () => {
            sidebar.classList.remove("open");
            scrim.classList.remove("visible");
        };
        toggle.addEventListener("click", () => {
            sidebar.classList.toggle("open");
            scrim.classList.toggle("visible");
        });
        scrim.addEventListener("click", close);
    }

    const showCoverFallback = image => {
        image.hidden = true;
        const fallback = image.parentElement.querySelector(".cover-placeholder");
        if (fallback) fallback.hidden = false;
    };

    document.querySelectorAll("img[data-book-cover]").forEach(image => {
        image.addEventListener("error", () => showCoverFallback(image), { once: true });
    });

    const makePlaceholder = title => {
        const placeholder = document.createElement("span");
        placeholder.className = "cover-placeholder";
        const initial = document.createElement("strong");
        initial.textContent = title.trim().charAt(0) || "B";
        const label = document.createElement("small");
        label.textContent = "Bibliotheca";
        placeholder.append(initial, label);
        return placeholder;
    };

    document.querySelectorAll("select[data-cover-select]").forEach(select => {
        const preview = document.querySelector(`[data-cover-preview="${select.dataset.coverSelect}"]`);
        if (!preview) return;
        const updatePreview = () => {
            const option = select.selectedOptions[0];
            preview.replaceChildren();
            if (!select.value) {
                const message = document.createElement("span");
                message.className = "cover-preview-empty";
                message.textContent = "Choose a title to preview its cover.";
                preview.append(message);
                return;
            }

            const title = option.dataset.title || option.textContent.trim();
            const frame = document.createElement("span");
            frame.className = "book-cover-frame selection-cover";
            if (option.dataset.cover) {
                const image = document.createElement("img");
                image.src = option.dataset.cover;
                image.alt = `Cover of ${title}`;
                image.loading = "lazy";
                image.decoding = "async";
                image.dataset.bookCover = "";
                image.addEventListener("error", () => showCoverFallback(image), { once: true });
                frame.append(image);
                const fallback = makePlaceholder(title);
                fallback.hidden = true;
                frame.append(fallback);
            } else {
                frame.append(makePlaceholder(title));
            }

            const copy = document.createElement("span");
            copy.className = "selection-book-copy";
            const name = document.createElement("strong");
            name.textContent = title;
            const author = document.createElement("small");
            author.textContent = option.dataset.author || "";
            copy.append(name, author);
            preview.append(frame, copy);
        };
        select.addEventListener("change", updatePreview);
        updatePreview();
    });
})();
