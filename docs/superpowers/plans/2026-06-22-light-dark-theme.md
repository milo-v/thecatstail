# Light/Dark Theme Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a warm beige/yellow-leaning light theme the app's default look, with the existing dark (slate) theme available via a persistent toggle button on every page.

**Architecture:** A `data-theme="light"|"dark"` attribute on `<html>` drives CSS custom properties already centralized in `tokens.css` (light values in `:root`, dark values in `[data-theme="dark"]`). Templates that currently hardcode neutral Tailwind classes (`bg-slate-900`, `bg-slate-800`, `border-slate-700/600`, `text-white`, `text-slate-200/300/400`) are mechanically swapped to new theme-aware utility classes (`bg-surface`, `bg-panel`, `border-surface`, `text-primary`, `text-muted`) backed by those custom properties. A shared `theme.js` flips the attribute and persists the choice to `localStorage`; a tiny inline script in each page's `<head>` applies the stored choice before first paint to avoid a flash of the wrong theme.

**Tech Stack:** Spring Boot (Kotlin) + Thymeleaf templates, Tailwind CDN (no build step), plain CSS custom properties, vanilla JS (no framework).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-06-22-light-dark-theme-design.md` — follow it exactly; this plan implements it.
- Element colors, the indigo accent, and semantic action colors (confirm/danger/switch/caution/secondary) do **not** change between themes — only neutral surface/text tokens do.
- Modal scrims (mulligan, dice-roll, game-over — `bg-black/80`–`/90`) and other transient floating overlays that already use a fixed dark background for contrast against variable content (damage-reaction badge, tuning-picker popout) are **left untouched** in both themes — they're independent of the base surface per the spec.
- No automated test framework exists for HTML/CSS/JS in this repo (it's a Kotlin/Spring Boot project; Thymeleaf templates and static assets aren't unit-tested). "Tests" in this plan are verification steps: `curl`/`grep` against the running app, and a Playwright visual check at the end. Each task still ends with a concrete, runnable verification — just not a `pytest`/JUnit assertion.
- Light theme colors: `--surface-bg:#f3e8d2`, `--surface-panel:#ead9b4`, `--surface-border:#cdb888`, `--text-primary:#3a2f1f`, `--text-muted:#8a7a5c`. Dark theme (current) colors: `--surface-bg:#0f172a`, `--surface-panel:#1e293b`, `--surface-border:#334155`, `--text-primary:#ffffff`, `--text-muted:#94a3b8`.
- No emoji in any markup or copy (project convention) — the toggle button is a text label ("Dark"/"Light"), not an icon.
- **Deviation from spec:** the spec's page list includes `loading.html`. On inspection, `loading.html` is a bare fragment (no `<html><head>`, no Tailwind/htmx scripts of its own) and is not referenced from any other template or route — `MainController.loading()` returns it, but nothing currently links to `/loading`. It also doesn't use the dark `bg-slate-900` theme at all (it has its own intentional light slate VS-splash palette, unrelated to this toggle). There is no `<head>` to add the anti-flash script to and no theme-relevant classes to swap, so it is excluded from this plan. If `loading.html` is wired up to a route later, theming it can be a follow-up.

---

### Task 1: Theme tokens in `tokens.css`

**Files:**
- Modify: `src/main/resources/static/css/tokens.css`

**Interfaces:**
- Produces: CSS custom properties `--surface-bg`, `--surface-panel`, `--surface-border`, `--text-primary`, `--text-muted` (light values in `:root`, dark overrides in `[data-theme="dark"]`), and utility classes `.bg-surface`, `.bg-panel`, `.border-surface`, `.text-primary`, `.text-muted`. All later tasks consume these class names.

- [ ] **Step 1: Replace the `:root` block and add the dark override + new utility classes**

Replace lines 9–57 of `tokens.css` (the `:root { ... }` block through the end of the existing `.font-display` rule) with:

```css
:root {
    /* Surfaces — light theme (default) */
    --surface-bg: #f3e8d2;
    --surface-panel: #ead9b4;
    --surface-border: #cdb888;
    --text-primary: #3a2f1f;
    --text-muted: #8a7a5c;

    /* Accent / brand */
    --accent: #6366f1;
    --accent-strong: #4f46e5;

    /* Semantic actions */
    --confirm: #16a34a;
    --danger: #dc2626;
    --switch: #2563eb;
    --caution: #ca8a04;
    --secondary: #334155;

    /* Glow / highlight */
    --glow: #facc15;

    /* Elements — bg variant (solid badges, dice, borders) */
    --el-pyro: #ef4444;
    --el-hydro: #3b82f6;
    --el-anemo: #14b8a6;
    --el-electro: #a855f7;
    --el-dendro: #22c55e;
    --el-cryo: #67e8f9;
    --el-geo: #eab308;
    --el-omni-start: #fde68a;
    --el-omni-end: #ffffff;
    --el-unaligned: #94a3b8;

    /* Elements — light text variant (floating predicted-element text over art) */
    --el-pyro-text: #ff9999;
    --el-hydro-text: #00ccff;
    --el-anemo-text: #7fffd4;
    --el-electro-text: #ffacff;
    --el-dendro-text: #77ff33;
    --el-cryo-text: #99ffff;
    --el-geo-text: #ffe699;

    /* Display typeface for banners, round counter, phase headers */
    --font-display: 'Rajdhani', sans-serif;
}

/* Dark theme override — applied when <html data-theme="dark"> */
[data-theme="dark"] {
    --surface-bg: #0f172a;
    --surface-panel: #1e293b;
    --surface-border: #334155;
    --text-primary: #ffffff;
    --text-muted: #94a3b8;
}

.font-display {
    font-family: var(--font-display);
}

/* Theme-aware neutral surfaces/text */
.bg-surface { background-color: var(--surface-bg); }
.bg-panel { background-color: var(--surface-panel); }
.border-surface { border-color: var(--surface-border); }
.text-primary { color: var(--text-primary); }
.text-muted { color: var(--text-muted); }
```

Leave everything from the old `/* Predicted-element floating text (light variant) */` comment onward (the `.text-pyro`/`.el-PYRO`/`.die`/`.card-glow-active`/`.hp-bar-*`/`.log-*` rules) exactly as it is — only the block above changes.

- [ ] **Step 2: Verify the file is well-formed**

Run: `cat src/main/resources/static/css/tokens.css`
Expected: one `:root` block, one `[data-theme="dark"]` block, both present exactly once; no duplicate `--surface-bg` declarations outside those two blocks.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/tokens.css
git commit -m "feat: add light/dark theme tokens to tokens.css"
```

---

### Task 2: Shared toggle script `theme.js`

**Files:**
- Create: `src/main/resources/static/js/theme.js`

**Interfaces:**
- Consumes: a DOM element `#theme-toggle` (a `<button>`) that later tasks add to each page; `document.documentElement`'s `data-theme` attribute (from Task 1).
- Produces: click-to-toggle behavior and `localStorage.theme` persistence. Later tasks only need to add the button markup and `<script src="/js/theme.js">` — no other JS API surface.

- [ ] **Step 1: Create the file**

```js
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
```

- [ ] **Step 2: Verify it's syntactically valid JS**

Run: `node --check src/main/resources/static/js/theme.js`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/theme.js
git commit -m "feat: add theme toggle script"
```

---

### Task 3: Wire up `game_page.html` (full-page game wrapper)

**Files:**
- Modify: `src/main/resources/templates/game_page.html`

**Interfaces:**
- Consumes: `.bg-surface`, `.text-primary` (Task 1), `theme.js` + `#theme-toggle` contract (Task 2).

- [ ] **Step 1: Add the anti-flash script as the first line of `<head>`**

Change:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
</head>
```
to:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail</title>
    <script>document.documentElement.setAttribute('data-theme', localStorage.getItem('theme') || 'light');</script>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
</head>
```

- [ ] **Step 2: Swap the body background and add the toggle button**

Change:
```html
<body class="bg-slate-900">
<div id="container">
    <div th:replace="~{game :: content}"></div>
</div>
</body>
```
to:
```html
<body class="bg-surface text-primary">
<div id="container">
    <div th:replace="~{game :: content}"></div>
</div>
<button id="theme-toggle" type="button"
        class="fixed top-2 right-2 z-40 w-8 h-8 rounded-full bg-panel border border-surface text-primary text-[10px] font-bold flex items-center justify-center shadow"
        aria-label="Toggle color theme">Dark</button>
<script src="/js/theme.js"></script>
</body>
```

The button lives outside `#container` so HTMX swaps targeting `#game-container` (inside the fragment) never remove it.

- [ ] **Step 2: Manual verification**

Run: `./gradlew bootRun` (in one terminal), then in another:
```bash
curl -s http://localhost:8000/game/setup -o /dev/null -w "%{http_code}\n"
curl -s http://localhost:8000/game_page.html 2>/dev/null; true # game_page is a view, not a static file — skip
```
Instead, after Task 6 (`game.html`) is also done, load `/game` in a browser (see Task 7's end-to-end check) and confirm the toggle button renders top-right and is clickable. Stop the server with Ctrl+C when done.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/game_page.html
git commit -m "feat: wire up theme toggle on game page wrapper"
```

---

### Task 4: Wire up `home.html`

**Files:**
- Modify: `src/main/resources/templates/home.html`

**Interfaces:**
- Consumes: same as Task 3.

- [ ] **Step 1: Add `tokens.css` link and the anti-flash script to `<head>`**

Change:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
```
to:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail</title>
    <script>document.documentElement.setAttribute('data-theme', localStorage.getItem('theme') || 'light');</script>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
</head>
```

- [ ] **Step 2: Swap the body background and add the toggle button**

Change:
```html
<body class="bg-gray-100 h-screen">
<div class="w-full h-full flex items-center justify-center" id="container">
    <button hx-get="/game" hx-swap="outerHTML" hx-target="#container" class="bg-indigo-600 text-white font-bold py-3 px-8 rounded-lg shadow-lg hover:bg-indigo-700 transition transform hover:scale-105">
        Start Single Player
    </button>
</div>

</body>
```
to:
```html
<body class="bg-surface text-primary h-screen">
<div class="w-full h-full flex items-center justify-center" id="container">
    <button hx-get="/game" hx-swap="outerHTML" hx-target="#container" class="bg-indigo-600 text-white font-bold py-3 px-8 rounded-lg shadow-lg hover:bg-indigo-700 transition transform hover:scale-105">
        Start Single Player
    </button>
</div>
<button id="theme-toggle" type="button"
        class="fixed top-2 right-2 z-40 w-8 h-8 rounded-full bg-panel border border-surface text-primary text-[10px] font-bold flex items-center justify-center shadow"
        aria-label="Toggle color theme">Dark</button>
<script src="/js/theme.js"></script>
</body>
```

Note the "Start Single Player" button keeps its literal `bg-indigo-600 text-white` — it's a fixed-color button, not part of the neutral surface swap.

- [ ] **Step 3: Manual verification**

```bash
./gradlew bootRun &
sleep 8
curl -s http://localhost:8000/ | grep -c 'theme-toggle'
curl -s http://localhost:8000/ | grep -c 'bg-surface'
kill %1
```
Expected: both `grep -c` calls print `1` or more.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/home.html
git commit -m "feat: wire up theme toggle on home page"
```

---

### Task 5: Wire up `setup.html`

**Files:**
- Modify: `src/main/resources/templates/setup.html`

**Interfaces:**
- Consumes: same as Task 3.

- [ ] **Step 1: Add the anti-flash script to `<head>`**

Change:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail - Setup</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
```
to:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail - Setup</title>
    <script>document.documentElement.setAttribute('data-theme', localStorage.getItem('theme') || 'light');</script>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
```

- [ ] **Step 2: Swap the body classes**

Change:
```html
<body class="bg-slate-900 text-white min-h-screen">
```
to:
```html
<body class="bg-surface text-primary min-h-screen">
```

- [ ] **Step 3: Swap caption text colors**

Change (two separate occurrences — make both edits):
```html
            <p class="text-sm text-slate-400">Pick 3 characters and build a 30-card deck.</p>
```
to:
```html
            <p class="text-sm text-muted">Pick 3 characters and build a 30-card deck.</p>
```

Change:
```html
            <h2 class="text-xl font-bold">Characters
                <span class="text-sm text-slate-400">(<span id="char-count">0</span>/3)</span>
            </h2>
```
to:
```html
            <h2 class="text-xl font-bold">Characters
                <span class="text-sm text-muted">(<span id="char-count">0</span>/3)</span>
            </h2>
```

Change:
```html
            <h2 class="text-xl font-bold">Deck
                <span class="text-sm text-slate-400">(<span id="deck-count">0</span>/30)</span>
            </h2>
```
to:
```html
            <h2 class="text-xl font-bold">Deck
                <span class="text-sm text-muted">(<span id="deck-count">0</span>/30)</span>
            </h2>
```

Change the disabled-state caption on the Start Match button:
```html
                    class="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:text-slate-400 text-white font-bold py-2 px-4 rounded">
```
to:
```html
                    class="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:text-muted text-white font-bold py-2 px-4 rounded">
```
(`disabled:bg-slate-700` stays — it's the fixed secondary/disabled button color, not the base surface.)

- [ ] **Step 4: Swap panel backgrounds for character and deck tiles**

Change:
```html
            <div th:each="c : ${characters}"
                 class="char-card cursor-pointer bg-slate-800 rounded p-2 text-center transition"
```
to:
```html
            <div th:each="c : ${characters}"
                 class="char-card cursor-pointer bg-panel rounded p-2 text-center transition"
```

Change:
```html
        <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2 max-h-[420px] overflow-y-auto p-1 bg-slate-950/40 rounded">
            <div th:each="card : ${cards}"
                 class="card-tile cursor-pointer bg-slate-800 rounded p-2 text-center hover:bg-slate-700 transition"
```
to:
```html
        <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2 max-h-[420px] overflow-y-auto p-1 bg-panel rounded">
            <div th:each="card : ${cards}"
                 class="card-tile cursor-pointer bg-panel rounded p-2 text-center hover:bg-slate-700 transition"
```

- [ ] **Step 5: Add the toggle button before `</body>`**

Change:
```html
</script>
</body>
</html>
```
to:
```html
</script>
<button id="theme-toggle" type="button"
        class="fixed top-2 right-2 z-40 w-8 h-8 rounded-full bg-panel border border-surface text-primary text-[10px] font-bold flex items-center justify-center shadow"
        aria-label="Toggle color theme">Dark</button>
<script src="/js/theme.js"></script>
</body>
</html>
```

- [ ] **Step 6: Manual verification**

```bash
./gradlew bootRun &
sleep 8
curl -s http://localhost:8000/game/setup | grep -c 'theme-toggle'
curl -s http://localhost:8000/game/setup | grep -c 'bg-panel'
kill %1
```
Expected: both print `1` or more (the second should be several, since multiple elements use `bg-panel`).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/setup.html
git commit -m "feat: wire up theme toggle on setup page"
```

---

### Task 6: Wire up `game.html` (board fragment)

**Files:**
- Modify: `src/main/resources/templates/game.html`

**Interfaces:**
- Consumes: same tokens/classes as Task 1. No toggle button here — `game.html` is an HTMX fragment swapped via `outerHTML`; the toggle button lives in the wrapping `game_page.html` (Task 3) so it survives swaps.

- [ ] **Step 1: Swap the fragment root background/text**

Change:
```html
<div class="h-screen w-screen flex flex-col bg-slate-900 text-white overflow-hidden relative"
     th:fragment="content" id="game-container">
```
to:
```html
<div class="h-screen w-screen flex flex-col bg-surface text-primary overflow-hidden relative"
     th:fragment="content" id="game-container">
```

- [ ] **Step 2: Make the support/summon slot base style theme-aware**

Change:
```css
        .support-slot, .summon-slot {
            width: 60px; height: 80px;
            background: rgba(30, 41, 59, 0.5);
            border: 1px dashed rgba(100, 116, 139, 0.5);
            border-radius: 4px;
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            font-size: 10px;
        }
```
to:
```css
        .support-slot, .summon-slot {
            width: 60px; height: 80px;
            background: var(--surface-panel);
            border: 1px dashed var(--surface-border);
            border-radius: 4px;
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            font-size: 10px;
        }
```

- [ ] **Step 3: Remove the conflicting hardcoded `bg-slate-800/80` on each support/summon slot element**

There are 4 occurrences of this pattern (opponent support, opponent summon, player support, player summon). For each, remove `bg-slate-800/80` from the class list so the themed base style from Step 2 applies instead of a hardcoded dark override.

Change (opponent support, around line 113):
```html
                    <div th:each="support : ${match.players[1].supportZone}" class="support-slot border-solid bg-slate-800/80 border-indigo-500/50"
```
to:
```html
                    <div th:each="support : ${match.players[1].supportZone}" class="support-slot border-solid border-indigo-500/50"
```

Change (opponent summon, around line 194):
```html
                    <div th:each="summon : ${match.players[1].summonsZone}" class="summon-slot border-solid bg-slate-800/80 border-red-500/50"
```
to:
```html
                    <div th:each="summon : ${match.players[1].summonsZone}" class="summon-slot border-solid border-red-500/50"
```

Change (player support, around line 225):
```html
                    <div th:each="support : ${match.players[0].supportZone}" class="support-slot border-solid bg-slate-800/80 border-indigo-500/50"
```
to:
```html
                    <div th:each="support : ${match.players[0].supportZone}" class="support-slot border-solid border-indigo-500/50"
```

Change (player summon, around line 309):
```html
                    <div th:each="summon : ${match.players[0].summonsZone}" class="summon-slot border-solid bg-slate-800/80 border-red-500/50"
```
to:
```html
                    <div th:each="summon : ${match.players[0].summonsZone}" class="summon-slot border-solid border-red-500/50"
```

- [ ] **Step 4: Swap the turn-indicator banner's opponent-turn colors**

Change:
```html
             th:classappend="${match.getActivePlayer().userId == userId} ? 'bg-green-600 text-white' : 'bg-slate-700 text-slate-300'"
```
to:
```html
             th:classappend="${match.getActivePlayer().userId == userId} ? 'bg-green-600 text-white' : 'bg-panel text-muted'"
```

- [ ] **Step 5: Swap the mulligan card-tile panel and its type label**

Change:
```html
                    <label th:each="card, iter : ${match.players[0].hand}"
                           class="w-24 h-32 bg-slate-800 border border-slate-600 rounded-md p-2 flex flex-col items-center justify-between cursor-pointer has-[:checked]:border-red-400 has-[:checked]:ring-2 has-[:checked]:ring-red-500">
                        <input type="checkbox" name="indices" th:value="${iter.index}" class="accent-red-500" />
                        <span class="text-[10px] font-bold text-center" th:text="${card.name}">Card</span>
                        <span class="text-[8px] text-slate-400" th:text="${card.type}">TYPE</span>
                    </label>
```
to:
```html
                    <label th:each="card, iter : ${match.players[0].hand}"
                           class="w-24 h-32 bg-panel border border-surface rounded-md p-2 flex flex-col items-center justify-between cursor-pointer has-[:checked]:border-red-400 has-[:checked]:ring-2 has-[:checked]:ring-red-500">
                        <input type="checkbox" name="indices" th:value="${iter.index}" class="accent-red-500" />
                        <span class="text-[10px] font-bold text-center" th:text="${card.name}">Card</span>
                        <span class="text-[8px] text-muted" th:text="${card.type}">TYPE</span>
                    </label>
```

- [ ] **Step 6: Swap the structural divider above the opponent zone**

Change:
```html
        <div class="h-1/2 flex flex-col p-4 border-b border-slate-800 bg-red-900/5">
```
to:
```html
        <div class="h-1/2 flex flex-col p-4 border-b border-surface bg-red-900/5">
```

- [ ] **Step 7: Swap character card panels and inactive borders (opponent, then player)**

Change (opponent character card, around line 123–125):
```html
                    <div th:each="char, iter : ${match.players[1].characters}"
                         class="relative p-2 border-2 rounded-lg bg-slate-800 flex flex-col items-center w-28 transition-all"
                         th:title="${char.name + ' — ' + char.element + ' (HP ' + char.currentHp + '/' + char.maxHp + ')'}"
                         th:classappend="${(iter.index == match.players[1].activeCharacterIndex ? 'card-glow-active' : 'border-slate-700') + (char.isAlive ? '' : ' grayscale opacity-50')}">
```
to:
```html
                    <div th:each="char, iter : ${match.players[1].characters}"
                         class="relative p-2 border-2 rounded-lg bg-panel flex flex-col items-center w-28 transition-all"
                         th:title="${char.name + ' — ' + char.element + ' (HP ' + char.currentHp + '/' + char.maxHp + ')'}"
                         th:classappend="${(iter.index == match.players[1].activeCharacterIndex ? 'card-glow-active' : 'border-surface') + (char.isAlive ? '' : ' grayscale opacity-50')}">
```

Change (opponent HP badge, around line 128):
```html
                        <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-slate-900 border-2 border-red-500 flex items-center justify-center font-bold text-sm"
                             th:text="${char.currentHp}">10</div>
```
to:
```html
                        <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-surface border-2 border-red-500 flex items-center justify-center font-bold text-sm"
                             th:text="${char.currentHp}">10</div>
```

Change (player character card, around line 235–237):
```html
                    <div th:each="char, iter : ${match.players[0].characters}"
                         class="relative p-2 border-2 rounded-lg bg-slate-800 flex flex-col items-center w-28 transition-all"
                         th:title="${char.name + ' — ' + char.element + ' (HP ' + char.currentHp + '/' + char.maxHp + ')'}"
                         th:classappend="${(iter.index == match.players[0].activeCharacterIndex ? 'card-glow-active' : 'border-slate-700') + (char.isAlive ? '' : ' grayscale opacity-50') + (previewSwitchIndex != null && previewSwitchIndex == iter.index ? ' ring-4 ring-blue-400 ring-offset-2 ring-offset-slate-900' : '') + (iter.index != match.players[0].activeCharacterIndex ? ' cursor-pointer hover:border-blue-400' : ' cursor-default')}"
```
to:
```html
                    <div th:each="char, iter : ${match.players[0].characters}"
                         class="relative p-2 border-2 rounded-lg bg-panel flex flex-col items-center w-28 transition-all"
                         th:title="${char.name + ' — ' + char.element + ' (HP ' + char.currentHp + '/' + char.maxHp + ')'}"
                         th:classappend="${(iter.index == match.players[0].activeCharacterIndex ? 'card-glow-active' : 'border-surface') + (char.isAlive ? '' : ' grayscale opacity-50') + (previewSwitchIndex != null && previewSwitchIndex == iter.index ? ' ring-4 ring-blue-400 ring-offset-2 ring-offset-slate-900' : '') + (iter.index != match.players[0].activeCharacterIndex ? ' cursor-pointer hover:border-blue-400' : ' cursor-default')}"
```

Change (player HP badge, around line 243):
```html
                        <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-slate-900 border-2 border-blue-500 flex items-center justify-center font-bold text-sm"
                             th:text="${char.currentHp}">10</div>
```
to:
```html
                        <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-surface border-2 border-blue-500 flex items-center justify-center font-bold text-sm"
                             th:text="${char.currentHp}">10</div>
```

- [ ] **Step 8: Swap the hand card panel**

Change:
```html
                <div th:each="card : ${match.players[0].hand}"
                     class="w-20 h-28 bg-slate-800 border border-slate-600 rounded-md p-1 flex flex-col justify-between hover:-translate-y-4 transition-transform cursor-pointer group relative overflow-visible"
```
to:
```html
                <div th:each="card : ${match.players[0].hand}"
                     class="w-20 h-28 bg-panel border border-surface rounded-md p-1 flex flex-col justify-between hover:-translate-y-4 transition-transform cursor-pointer group relative overflow-visible"
```

- [ ] **Step 9: Swap the skill tooltip panel and its description text**

Change:
```html
                        <div class="absolute bottom-full mb-4 w-64 p-3 bg-slate-800 border border-slate-600 rounded-lg shadow-2xl invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-all duration-300 delay-1000 z-50 pointer-events-none">
                            <span class="block font-bold text-indigo-400 mb-1" th:text="${skill.name}">Skill Name</span>
                            <span class="text-[10px] text-slate-300 leading-relaxed" th:text="${skill.description}">Description text goes here...</span>
                        </div>
```
to:
```html
                        <div class="absolute bottom-full mb-4 w-64 p-3 bg-panel border border-surface rounded-lg shadow-2xl invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-all duration-300 delay-1000 z-50 pointer-events-none">
                            <span class="block font-bold text-indigo-400 mb-1" th:text="${skill.name}">Skill Name</span>
                            <span class="text-[10px] text-muted leading-relaxed" th:text="${skill.description}">Description text goes here...</span>
                        </div>
```

- [ ] **Step 10: Swap the skill cost label text color**

Change:
```html
                        <div class="flex gap-0.5 mt-1">
                            <span th:each="cost : ${skill.cost}" class="text-[8px] font-bold text-slate-300">
                                <span th:text="${cost.amount}"></span><span th:text="${cost.element}"></span>
                            </span>
                        </div>
```
to:
```html
                        <div class="flex gap-0.5 mt-1">
                            <span th:each="cost : ${skill.cost}" class="text-[8px] font-bold text-muted">
                                <span th:text="${cost.amount}"></span><span th:text="${cost.element}"></span>
                            </span>
                        </div>
```

- [ ] **Step 11: Swap the log panel and nudge it down to clear the toggle button**

Change:
```html
            <div class="absolute top-4 right-4 w-64 h-32 bg-slate-800/50 rounded overflow-y-auto p-2 text-[10px] font-mono border border-slate-700/50" id="game-log">
```
to:
```html
            <div class="absolute top-12 right-4 w-64 h-32 bg-panel rounded overflow-y-auto p-2 text-[10px] font-mono border border-surface" id="game-log">
```

- [ ] **Step 12: Verify the changes that should NOT have been made are still intact**

Run:
```bash
grep -n 'bg-slate-900/90\|bg-slate-900/95\|text-slate-200\|text-slate-300' src/main/resources/templates/game.html
```
Expected output (these are the intentionally-untouched scrim/art-overlay/transient-float instances):
```
60:            <p class="text-sm text-slate-300">Select cards to return to the bottom of your deck.</p>
79:            <p class="text-sm text-slate-300">Select dice to reroll, then confirm.</p>
102:            <p class="text-xl text-slate-200">Winner: <span th:text="${match.winner}">user</span></p>
145:                                  class="text-xs font-bold text-yellow-400 px-1 bg-slate-900/90 rounded border border-yellow-400/50 shadow-lg"
257:                                  class="text-xs font-bold text-yellow-400 px-1 bg-slate-900/90 rounded border border-yellow-400/50 shadow-lg"
334:                        <span class="text-[8px] text-slate-200 drop-shadow-md" th:text="${card.type}">SUP</span>
342:                        <div class="flex gap-1 bg-slate-900/95 border border-yellow-500/50 rounded-full p-1 shadow-lg">
```
(Exact line numbers may drift slightly from earlier edits — confirm the same 7 lines/contexts are present, not the exact numbers.)

- [ ] **Step 13: Verify no stray `bg-slate-900`/`bg-slate-800`/`border-slate-700`/`border-slate-600` remain outside the allowed exceptions**

Run:
```bash
grep -n 'bg-slate-900[^/]\|bg-slate-800[^/]\|border-slate-700[^/]\|border-slate-600' src/main/resources/templates/game.html
```
Expected: no matches (empty output). If anything prints, it was missed in Steps 1–11 — fix it before moving on.

- [ ] **Step 14: Commit**

```bash
git add src/main/resources/templates/game.html
git commit -m "feat: make game board theme-aware"
```

---

### Task 7: Wire up `styleguide.html`

**Files:**
- Modify: `src/main/resources/static/styleguide.html`

**Interfaces:**
- Consumes: same tokens/classes as Task 1, same toggle contract as Task 2.

- [ ] **Step 1: Add the anti-flash script to `<head>`**

Change:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail — Style Guide</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
```
to:
```html
<head>
    <meta charset="UTF-8">
    <title>The Cat's Tail — Style Guide</title>
    <script>document.documentElement.setAttribute('data-theme', localStorage.getItem('theme') || 'light');</script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/tokens.css">
```

- [ ] **Step 2: Swap the body classes**

Change:
```html
<body class="bg-slate-900 text-white min-h-screen">
```
to:
```html
<body class="bg-surface text-primary min-h-screen">
```

- [ ] **Step 3: Swap every reference-box panel background from `bg-slate-800` to `bg-panel`**

There are 13 occurrences of `bg-slate-800` used as a plain reference-box background (typography cards, surfaces/semantic-colors swatch captions live in their own divs not these, element boxes, the two character-card demo panels, the hand-card demo, the z-index tier boxes). Use a project-wide replace for this exact file:

```bash
sed -i 's/bg-slate-800 rounded-lg p-4/bg-panel rounded-lg p-4/g; s/bg-slate-800 rounded-lg p-3 flex flex-col items-center gap-2/bg-panel rounded-lg p-3 flex flex-col items-center gap-2/g; s/bg-slate-800 rounded p-3/bg-panel rounded p-3/g' src/main/resources/static/styleguide.html
```

Then handle the two character-card panels and the hand-card panel individually, since they have distinct surrounding classes:

Change:
```html
            <div class="relative p-2 border-2 rounded-lg bg-slate-800 flex flex-col items-center w-28 card-glow-active">
```
to:
```html
            <div class="relative p-2 border-2 rounded-lg bg-panel flex flex-col items-center w-28 card-glow-active">
```

Change:
```html
            <div class="relative p-2 border-2 border-slate-700 rounded-lg bg-slate-800 flex flex-col items-center w-28">
```
to:
```html
            <div class="relative p-2 border-2 border-surface rounded-lg bg-panel flex flex-col items-center w-28">
```

Change:
```html
        <div class="w-20 h-28 bg-slate-800 border border-slate-600 rounded-md p-1 flex flex-col justify-between">
```
to:
```html
        <div class="w-20 h-28 bg-panel border border-surface rounded-md p-1 flex flex-col justify-between">
```

- [ ] **Step 4: Swap the two HP badge circles in the character-card demo**

Change (both occurrences — identical string, use `replace_all` if using an editor, or `sed`):
```html
                <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-slate-900 border-2 border-blue-500 flex items-center justify-center font-bold text-sm">8</div>
```
to:
```html
                <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-surface border-2 border-blue-500 flex items-center justify-center font-bold text-sm">8</div>
```
and:
```html
                <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-slate-900 border-2 border-blue-500 flex items-center justify-center font-bold text-sm">2</div>
```
to:
```html
                <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-surface border-2 border-blue-500 flex items-center justify-center font-bold text-sm">2</div>
```

- [ ] **Step 5: Remove the conflicting hardcoded backgrounds on the support/summon slot demo**

Change:
```html
                <div class="support-slot border-solid bg-slate-800/80 border-indigo-500/50">
```
to:
```html
                <div class="support-slot border-solid border-indigo-500/50">
```

Change:
```html
                <div class="summon-slot border-solid bg-slate-800/80 border-red-500/50">
```
to:
```html
                <div class="summon-slot border-solid border-red-500/50">
```

(The `.support-slot`/`.summon-slot` base CSS rule was already made theme-aware in Task 6, Step 2 — this file uses the same `tokens.css`, so no further CSS change is needed here.)

- [ ] **Step 6: Swap the log demo panel**

Change:
```html
        <div class="w-64 h-28 bg-slate-800/50 rounded overflow-y-auto p-2 text-[10px] font-mono border border-slate-700/50">
```
to:
```html
        <div class="w-64 h-28 bg-panel rounded overflow-y-auto p-2 text-[10px] font-mono border border-surface">
```

- [ ] **Step 7: Swap caption/muted text colors**

Change every occurrence of `text-slate-400` and `text-slate-300` used as captions in this file (NOT `text-slate-200`, which is the hand-card "SUP" label sitting on a small chip and stays as-is for consistency with `game.html`'s equivalent label) to `text-muted`:

```bash
sed -i 's/text-slate-400/text-muted/g; s/text-slate-300/text-muted/g' src/main/resources/static/styleguide.html
```

- [ ] **Step 8: Add the toggle button before `</body>`**

Change:
```html
</section>

</div>
</body>
</html>
```
to:
```html
</section>

</div>
<button id="theme-toggle" type="button"
        class="fixed top-2 right-2 z-40 w-8 h-8 rounded-full bg-panel border border-surface text-primary text-[10px] font-bold flex items-center justify-center shadow"
        aria-label="Toggle color theme">Dark</button>
<script src="/js/theme.js"></script>
</body>
</html>
```

- [ ] **Step 9: Verify no stray dark-only classes remain**

```bash
grep -n 'bg-slate-900[^/]\|bg-slate-800\|border-slate-700\|text-slate-400\|text-slate-300' src/main/resources/static/styleguide.html
```
Expected: empty output.

- [ ] **Step 10: Commit**

```bash
git add src/main/resources/static/styleguide.html
git commit -m "feat: make style guide theme-aware"
```

---

### Task 8: End-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Start the app**

```bash
cd /home/milo/dev/thecatstail
./gradlew bootRun > /tmp/bootrun.log 2>&1 &
for i in $(seq 1 30); do curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/ && break; sleep 2; done
```
Expected: prints `200`.

- [ ] **Step 2: Confirm the light theme is the default on every page**

```bash
for path in "/" "/game/setup" "/styleguide.html"; do
  echo "== $path =="
  curl -s "http://localhost:8000$path" | grep -o "localStorage.getItem('theme') || 'light'"
done
```
Expected: each page prints the match once (confirms the anti-flash default is `'light'`).

- [ ] **Step 3: Visual check with Playwright (or any browser)**

Navigate to `http://localhost:8000/`, then `/game/setup`, then start a quick match via Quick Start, then `/styleguide.html`. For each:
- Confirm the page renders in the warm beige light theme with readable text and visible panel/border contrast.
- Click the top-right `theme-toggle` button; confirm it switches the whole page to the dark slate theme immediately, with all element colors/dice/badges/buttons unchanged.
- Reload the page; confirm it stays on the theme you last picked (no flash of the other theme).
- On the game board specifically: confirm the log panel (top-right) and the toggle button (further top-right, fixed) no longer overlap.

- [ ] **Step 4: Stop the app**

```bash
kill %1 2>/dev/null
```

- [ ] **Step 5: Final commit (if any cleanup was needed)**

```bash
git status --short
```
Expected: clean working tree (everything was already committed per-task). If anything is still unstaged, commit it now with a descriptive message.
