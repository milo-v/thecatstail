# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (dev)
./gradlew bootRun

# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "io.github.milov.thecatstail.SomeTestClass"

# Docker (app + MongoDB)
docker-compose up
```

Server runs on port 8000.

## Architecture

This is a **Genshin Impact TCG simulation engine** — a turn-based tactical card game with a Spring Boot backend, Thymeleaf+HTMX frontend, and an MCTS AI opponent.

### Layers

```
presentation/       Spring MVC controllers + Thymeleaf templates
application/        Service orchestration, CQRS command/query bus
domain/             Core game logic and models (no framework deps)
infrastructure/     Concrete implementations (in-memory repos, bus, data loading)
```

### Domain Logic (`domain/logic/`)

- **`MatchManager`** — turn/phase management (ROLL → ACTION → END phases), applies moves, transitions state
- **`CombatEngine`** — damage calculation, elemental application, piercing damage, swirl spread
- **`ReactionManager`** — elemental reaction system (Vaporize, Melt, Frozen, Bloom, Swirl, etc.)
- **`MoveGenerator`** — computes all legal moves for the active player
- **`CostValidator`** — validates dice pool satisfies a skill/card's `DiceCost`
- **`DiceEngine`** — dice rolling mechanics
- **`StatusEngine`** — applies/ticks/expires character and combat statuses each phase transition
- **`CardEffectRegistry`** — maps action card IDs to their effect implementations
- **`StatusEffectRegistry`** — maps status effect IDs to their effect implementations
- **`SummonEffectRegistry`** — maps summon IDs to their on-trigger effect implementations
- **`ai/MctsBot`** — Monte Carlo Tree Search AI (500 iterations, UCT selection)

### Key Domain Models (`domain/model/`)

```
Match
├── players: List<PlayerState>
│   └── PlayerState
│       ├── characters: List<Character>   (with hp, energy, appliedElements, statuses)
│       ├── hand / deck: List<ActionCard>
│       ├── dicePool: Map<Element, Int>
│       ├── supportZone: List<SupportCard>
│       └── summonsZone: List<Summon>
├── activePlayerIndex
├── phase: ROLL | ACTION | END
├── roundNumber
└── eventLog: List<GameEvent>
```

**Enums:** `Element` (PYRO/HYDRO/ANEMO/ELECTRO/DENDRO/CRYO/GEO/OMNI/UNALIGNED), `SkillType` (NORMAL_ATTACK/ELEMENTAL_SKILL/ELEMENTAL_BURST), `Phase`

**Player actions (Move subtypes):** `UseSkill`, `SwitchCharacter`, `PlayCard`, `ElementalTuning`, `DeclareEnd`

### Data Loading

Game data is loaded at startup from JSON files in `src/main/resources/data/` by `TcgDataLoader` (infrastructure), then stored in in-memory repositories: `InMemCharacterRepository`, `InMemDeckRepository`, `InMemStatusRepository`, `InMemSummonRepository`.

Card data comes from the `theBowja/genshin-db-dist` repo. To pull a new game version's data, run `fetch_data.sh` — see `docs/TCG_DATA_UPDATE_GUIDE.md` for the extraction/mapping details and maintenance steps.

### Application Layer — CQRS

`application/base/` defines `Command`/`Query` interfaces and their buses. All game actions flow through `SinglePlayerMatchService`, which delegates to `MatchManager` and invokes `MctsBot` for the AI player's response after each human move.

### Frontend

Thymeleaf templates with HTMX for partial page updates. `game.html` is the main game board; actions POST to `GameController` which returns HTML fragments.
