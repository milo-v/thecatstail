# UI Design Guide

This documents the visual system for The Cat's Tail's frontend (Thymeleaf + HTMX + Tailwind CDN). It exists so new templates and components look like they belong to the same game instead of drifting page by page — which is what had already started happening (see History below).

Reference implementation: `src/main/resources/static/css/tokens.css`. Live demo: `/styleguide.html`.

## Principles

- **The board is a HUD, not a brochure.** Low-chrome panels and small bold labels — the UI should feel like a tactical overlay on top of character art, not a marketing page. The app ships two themes: a warm beige/yellow light theme (the default) and a dark slate theme, switchable at any time via a persistent toggle button. Both should read as the same tactical HUD, just lit differently — surfaces and borders shift hue/value between themes, but panel density, chrome, and label treatment don't.
- **Element color is the spine.** The nine elements (Pyro, Hydro, Anemo, Electro, Dendro, Cryo, Geo, Omni, Unaligned) are the single most distinctive, recurring visual language in the app — dice, badges, predicted-element text, character borders. Every other color (accent, semantic) exists to support it, not compete with it. There must be exactly one definition per element color, in `tokens.css`.
- **Bold sans for data, condensed display for moments.** Default Tailwind sans (`font-black`, `tracking-widest`) carries labels, numbers, and HUD text — it's compact and reads well at small sizes. A single display face (Rajdhani) is reserved for the handful of moments where the game wants to feel like it's making an announcement: round counter, phase banners, mulligan/dice-roll headers, victory/defeat. Don't spread it onto buttons or body text — restraint is what makes it land.
- **Motion is functional, not decorative.** The only animations in the app communicate game state changes (damage floating up, a spinner during HTMX requests). New motion should follow that pattern — tied to something happening, not ambient flourish.

## Tokens

All values below are CSS custom properties in `tokens.css`. Use the variable, never hardcode the hex.

### Surfaces

Surface and text tokens are theme-aware: light values live in `:root` (default), dark values override them in `[data-theme="dark"]`. See "Light/dark theme" below for the mechanism.

| Token | Light (default) | Dark | Use |
|---|---|---|---|
| `--surface-bg` | `#f3e8d2` | `#0f172a` (slate-900) | Page/board background |
| `--surface-panel` | `#ead9b4` | `#1e293b` (slate-800) | Cards, panels, hand cards |
| `--surface-border` | `#cdb888` | `#334155` (slate-700) | Default borders |
| `--text-primary` | `#3a2f1f` | `#ffffff` | Primary text |
| `--text-muted` | `#8a7a5c` | `#94a3b8` (slate-400) | Secondary/caption text |

### Light/dark theme

Theme is controlled by a `data-theme="light"|"dark"` attribute on `<html>`. `tokens.css` defines light values as the `:root` defaults and overrides the theme-aware tokens (`--surface-bg`, `--surface-panel`, `--surface-border`, `--text-primary`, `--text-muted`) inside a `[data-theme="dark"]` block:

```css
[data-theme="dark"] { --surface-bg: #0f172a; /* …and the other theme-aware tokens */ }
```

A fixed top-right button (`#theme-toggle`, present on every full page) flips the attribute and persists the choice to `localStorage`, so the selected theme survives reloads and applies before first paint. Non-theme-aware tokens (accent, semantic actions, element colors, glow) are intentionally unchanged across themes — only neutral surfaces and text shift.

### Accent & semantic actions

| Token | Value | Use |
|---|---|---|
| `--accent` / `--accent-strong` | `#6366f1` / `#4f46e5` | Primary buttons, headers, branding (indigo) |
| `--confirm` | `#16a34a` | Confirm/positive actions (confirm attack, confirm dice) |
| `--danger` | `#dc2626` | Danger/terminal actions (End Phase) |
| `--switch` | `#2563eb` | Switch-character actions |
| `--caution` | `#ca8a04` | Dice reroll / caution actions |
| `--secondary` | `#334155` | Cancel/secondary buttons |
| `--glow` | `#facc15` | Active-character glow, selected-dice ring |

### Elements

Each element has a **bg variant** (solid badges, dice, card borders) and, where it's used as floating text over character art, a lighter **text variant** for contrast against busy backgrounds.

| Element | bg variant | text variant |
|---|---|---|
| Pyro | `--el-pyro` `#ef4444` | `--el-pyro-text` `#ff9999` |
| Hydro | `--el-hydro` `#3b82f6` | `--el-hydro-text` `#00ccff` |
| Anemo | `--el-anemo` `#14b8a6` | `--el-anemo-text` `#7fffd4` |
| Electro | `--el-electro` `#a855f7` | `--el-electro-text` `#ffacff` |
| Dendro | `--el-dendro` `#22c55e` | `--el-dendro-text` `#77ff33` |
| Cryo | `--el-cryo` `#67e8f9` | `--el-cryo-text` `#99ffff` |
| Geo | `--el-geo` `#eab308` | `--el-geo-text` `#ffe699` |
| Omni | gradient `--el-omni-start → --el-omni-end` (`#fde68a → #fff`) | — |
| Unaligned | `--el-unaligned` `#94a3b8` | — |

Apply via the existing utility classes — `.el-PYRO`, `.el-HYDRO`, … for solid backgrounds (badges, dice, `setup.html`'s `.element-*` swatches), `.text-pyro`, `.text-hydro`, … for the predicted-element floating text in `game.html`.

### Typography

| Role | Family | Where |
|---|---|---|
| Display | `var(--font-display)` → Rajdhani | Round counter, turn banner, mulligan/dice-roll headers, victory/defeat, page `<h1>`s |
| Body / UI | Tailwind default sans | Everything else — labels, buttons, card text, tooltips |
| Data / log | `font-mono` (Tailwind default) | Event log entries only |

Apply the display face with the `.font-display` class alongside the existing `font-black tracking-widest` weight/spacing utilities — it's additive, not a replacement for the existing scale.

### Z-index tiers

The overlay stack already doesn't collide; this just names the existing numbers so future overlays slot in predictably instead of picking an arbitrary number.

| Tier | Range | Example |
|---|---|---|
| HUD | 10–40 | Persistent corner widgets (round info, log) |
| Banner | 50 | Turn indicator |
| Preview | 100 | Damage / switch preview floats |
| Phase overlay | 150–200 | Dice roll phase, mulligan |
| Result | 300 | Game over |

## Component catalog

See `/styleguide.html` for a live, visual version of every item below.

- **Element badge** (`.el-badge` + `.el-<ELEMENT>`) — small pill, 2-letter element code. Used on character portraits, applied-element stacks, dice tooltips.
- **Die** (`.die` + `.el-<ELEMENT>`) — circular token, same element coloring as badges, used in the dice pool and tuning picker.
- **Character card** — `bg-slate-800` panel, HP chip top-left, element badge top-right, HP bar, energy dots (filled `●` / empty `○`, `--glow` colored when filled), status chips, `.card-glow-active` border when it's the active character.
- **Support / Summon slot** (`.support-slot`, `.summon-slot`) — dashed-border placeholder when empty, filled with name + remaining-uses count when occupied.
- **HP bar** (`.hp-bar-bg` / `.hp-bar-fill`, `.low` modifier) — green-to-lime gradient above 1/3 HP, red-to-amber below.
- **Status chip** — small indigo-tinted rounded label with name + remaining duration.
- **Log entry** — color-coded by category (`.log-damage`, `.log-reaction`, `.log-switch`, `.log-energy`, `.log-skill`, `.log-info`), monospace.
- **Buttons** — pill-shaped (`rounded-full`), color signals intent: indigo = primary/skill, green = confirm, red = danger/end, blue = switch, yellow = reroll/caution, slate = cancel/secondary.
- **Hand card** — portrait background, cost chip top-left, type label top-right, tuning picker revealed on hover.
- **Phase overlay** — full-bleed `bg-black/80` or `/90` scrim with centered content; use the z-index tiers above to decide placement.

## How to extend

- **Adding a status or summon visual variant**: reuse `--surface-panel`/`--surface-border` for the chip background; don't introduce a new neutral gray.
- **Adding a new action button**: pick the semantic token that matches its intent (confirm/danger/switch/caution/secondary) rather than a one-off color — if none fit, that's a sign the action's intent isn't clear yet.
- **Adding a new overlay**: place it in the z-index tier that matches its role (HUD/banner/preview/phase-overlay/result) instead of picking an arbitrary number.
- Never hardcode an element hex value in a template — add it to `tokens.css` if it's missing, then reference the variable or utility class.

## History

Before this guide, element colors were defined twice and had drifted: `game.html`'s Cryo badge was `#67e8f9` while `setup.html`'s was `#06b6d4`; Unaligned was `#94a3b8` vs `#64748b`. `tokens.css` now owns these values; `game.html` and `setup.html` both consume it.
