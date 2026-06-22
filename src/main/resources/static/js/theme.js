(function () {
    var btn = document.getElementById('theme-toggle');
    if (!btn) return;

    function currentTheme() {
        return document.documentElement.getAttribute('data-theme') || 'light';
    }

    function render() {
        btn.textContent = currentTheme() === 'light' ? 'Dark' : 'Light';
    }

    btn.addEventListener('click', function () {
        var next = currentTheme() === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
        render();
    });

    render();
})();
