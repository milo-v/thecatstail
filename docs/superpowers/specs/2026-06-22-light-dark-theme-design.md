# Light/Dark Theme Toggle

## Purpose

The app currently has one hardcoded dark theme (`bg-slate-900` + white text, applied directly via Tailwind utility classes in every template). The user wants a warm beige/yellow-leaning **light theme as the new default**, with the existing dark theme kept as an optional toggle, persisted across visits.

## Scope

- Element colors (Pyro/Hydro/Anemo/Electro/Dendro/Cryo/Geo/Omni/Unaligned), the indigo accent, and the semantic action colors (confirm/danger/switch/caution/secondary) are unchanged in both themes — they're already the app's settled visual spine (see `docs/UI_DESIGN_GUIDE.md`) and read fine on both backgrounds.
- Only **neutral** tokens flip: page/panel backgrounds, borders, primary/muted text.
- Full-bleed modal scrims (mulligan, dice-roll, game-over — currently literal `bg-black/80`–`/90`) are left as-is in both themes. They're independent dramatic overlays, not part of the base surface.
- Pages touched: `game.html` (+ `game_page.html` wrapper), `setup.html`, `home.html`, `loading.html`, `styleguide.html`.

## Token changes (`tokens.css`)

Add a `data-theme` attribute scope. Light is the default (`:root` / `[data-theme="light"]`); dark is `[data-theme="dark"]`.

| Token | Light (default) | Dark (current) |
|---|---|---|
| `--surface-bg` | `#f3e8d2` | `#0f172a` |
| `--surface-panel` | `#ead9b4` | `#1e293b` |
| `--surface-border` | `#cdb888` | `#334155` |
| `--text-primary` | `#3a2f1f` | `#ffffff` |
| `--text-muted` | `#8a7a5c` | `#94a3b8` |

New utility classes consuming these tokens, added to `tokens.css`:
- `.bg-surface` → `background-color: var(--surface-bg)`
- `.bg-panel` → `background-color: var(--surface-panel)`
- `.border-surface` → `border-color: var(--surface-border)`
- `.text-primary` → `color: var(--text-primary)`
- `.text-muted` → `color: var(--text-muted)` (already exists as a var reference for `.log-info`; this adds a directly-usable class)

## Template refactor (mechanical)

Replace hardcoded neutral Tailwind classes with the new utility classes, one-for-one:
- `bg-slate-900` (page/body background) → `bg-surface`
- `bg-slate-800` (panels: character cards, hand cards, log box, setup tiles, dice-roll/mulligan card tiles) → `bg-panel`
- `border-slate-700`, `border-slate-600` → `border-surface`
- `text-white` (default body text) → `text-primary`
- `text-slate-200`, `text-slate-300`, `text-slate-400` (captions/secondary text) → `text-muted`

Leave untouched: all `el-*` classes, accent/semantic button colors, `.die`, `.el-badge`, `.hp-bar-fill`, `.card-glow-active`, log category colors (`.log-damage` etc.), and all modal scrim backgrounds (`bg-black/...`).

`support-slot`/`summon-slot` in `game.html`'s local `<style>` currently use hardcoded `rgba(30,41,59,0.5)` background and `rgba(100,116,139,0.5)` border. Replace both with direct (non-alpha) references — `background: var(--surface-panel)` and `border: 1px dashed var(--surface-border)` — so they flip with the theme. Dropping the alpha is fine here: the slot already reads as a distinct panel against the board background in both themes without needing transparency.

## Toggle mechanism

- A small fixed-position button, top-right corner, present on every full page (`game_page.html`, `setup.html`, `home.html`, `loading.html`, `styleguide.html`).
- Markup: a `<button id="theme-toggle">` styled with `.bg-panel .border-surface .text-primary`, circular, ~32px, `position: fixed; top: 0.5rem; right: 0.5rem; z-index: 40` (HUD tier per the existing z-index convention).
- Behavior (in a new shared `static/js/theme.js`, loaded on every page after the toggle button markup):
  - On click: reads current `data-theme` from `<html>`, flips it, writes the new value to `localStorage.theme`, updates `<html data-theme="...">`.
  - Button label/icon reflects the *other* theme (i.e. what clicking will switch to) — text content "Dark" when currently light, "Light" when currently dark. No emoji (per project convention).
- Anti-flash: each page's `<head>` gets a small inline `<script>` (before the Tailwind CDN script, matching the existing per-page duplication pattern already used for htmx/Tailwind tags) that runs synchronously:
  ```html
  <script>
    document.documentElement.setAttribute('data-theme', localStorage.getItem('theme') || 'light');
  </script>
  ```
  This guarantees the correct theme is set before first paint, avoiding a flash of the wrong theme.

## `game.html` layout adjustment

The existing log panel (`absolute top-4 right-4 w-64 h-32`) sits in the same corner as the new toggle button. Nudge the log panel down to `top-12` (from `top-4`) so the fixed toggle button (which sits outside `#container`, always on top) doesn't visually collide with it. No other layout changes.

## Out of scope

- No animated/gradient background — the light theme uses plain solid warm-beige colors, not a shifting gradient (clarified with user: "plain colors, I just meant to describe the color is beige with warm yellow tendency").
- No per-user server-side persistence — `localStorage` only, no backend/session changes.
- No toggle inside modal overlays (mulligan/dice-roll/game-over) — the fixed button is visually behind those scrims while they're open, which is acceptable since the user's attention is on the overlay's own decision at that point.

## Verification

- `./gradlew bootRun`, then for each of `/`, `/game/setup`, `/game`, `/styleguide.html`:
  - Confirm the page loads in the warm beige light theme by default with readable contrast everywhere (panels, borders, text, buttons).
  - Click the toggle; confirm it switches to the original dark theme instantly, with element colors/badges/dice/buttons unchanged.
  - Reload the page; confirm the chosen theme persists (no flash of the other theme).
  - Play through mulligan → dice roll → a turn on the game board in both themes to confirm no regressions in legibility or the log-panel/toggle-button corner.
