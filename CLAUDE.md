# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See the root [`CLAUDE.md`](../CLAUDE.md) for multi-module build commands, environment variables, cross-cutting patterns, and dependency details.

## Build & Test

```bash
# From repo root
./gradlew :bot:build          # Build (includes shadowJar)
./gradlew :bot:test           # Run all tests
./gradlew :bot:test --tests "dev.sbs.bot.optimizer.OptimizerTest"  # Single test class

# Fat JAR (shadowJar merged into build task)
./gradlew :bot:shadowJar      # Output: build/libs/bot-0.1.0.jar
```

Tests require a live MariaDB database and valid `HYPIXEL_API_KEY` - they are integration tests that hit real APIs. A `TestLifecycleListener` (JUnit platform listener) manages `MinecraftApi` startup/shutdown so the test JVM exits cleanly.

## Module Overview

`bot` is the Discord bot application. It depends on `discord-api` and `minecraft-api` via Maven coordinates (not project references). All shared infrastructure (Gson, Scheduler, SessionManager, repositories) is reached through `MinecraftApi`.

### Entry Points

- **`SimplifiedBot`** - Main bot entry point. Extends `DiscordBot`, configures Discord gateway, connects to MariaDB via `JpaConfig.commonSql()`, schedules Hypixel cache updates.
- **`SimplifiedConsole`** - Secondary entry point for a resource-processing console bot. Runs `ResourceProcessor` tasks on a schedule to sync Hypixel resource API data into the database.

### Package Structure

**`command/`** - Discord slash commands. Two categories:
- Commands extending **`SkyBlockUserCommand`** (most commands) - automatically resolve a Minecraft player via the `name` and `profile` parameters, construct a `SkyBlockUser`, then delegate to `subprocess()`.
- Commands extending **`DiscordCommand`** directly (e.g. `AboutCommand`, `HelpCommand`) - general commands not tied to a SkyBlock player.
- `command/developer/` - Developer-only commands (`@Structure(developerOnly = true)`)
- `command/embed/` - Embed management commands
- `command/reputation/` - Reputation system commands

**`optimizer/`** - OptaPlanner-based reforge optimizer. Solves for best reforge allocation across armor, weapon, and accessories to maximize either damage-per-hit or damage-per-second.
- `Optimizer` - Static facade; owns two `SolverManager` instances (DPH and DPS)
- `modules/common/` - Shared abstractions: `Solution` (planning solution), `ItemEntity` (planning entity), `ReforgeFact` (planning fact), `Calculator` (score calculator)
- `modules/damage_per_hit/` and `modules/damage_per_second/` - Concrete implementations for each optimization type
- `util/OptimizerRequest` - Builder-pattern request object; resolves player profile, loads weapon from inventory/ender chest/backpacks by item ID
- `util/OptimizerSolver` - Programmatic `SolverConfig` builder (alternative to XML config)
- `util/OptimizerHelper` - Computes damage multipliers, enchant bonuses, armor bonuses, pet ability bonuses
- XML solver configs in `src/main/resources/optaplanner/`

**`profile_stats/`** - Player stat calculation engine.
- `ProfileStats` - Aggregates all stat sources (skills, slayers, dungeons, armor, accessories, pets, potions, century cakes, essence perks, mining core, etc.) into a typed `StatData` map keyed by `ProfileStats.Type` enum. Supports bonus stat calculation (enchantment multipliers, pet ability percentages).
- `data/` - Data abstractions: `Data` (base/bonus stat holder), `StatData`, `ItemData`, `AccessoryData`, `ObjectData`, `PlayerDataHelper`

**`model/`** - Bot-specific JPA entities stored in MariaDB (not embedded H2). These implement `JpaModel` directly with manual `equals()`/`hashCode()`. Examples: `AppUser`, `AppGuild`, `AppEmoji`, `OptimizerMobType`, `SkyBlockEvent`.

**`data/skyblock/`** - Supplementary SkyBlock data models (also JPA entities). These extend the `minecraft-api` model layer with additional bonus/shop data: `BonusArmorSetModel`, `BonusItemStatModel`, `BonusReforgeStatModel`, `HotPotatoStatModel`, `ShopBitTypeModel`, etc.

**`processor/`** - `Processor<R>` base class for syncing Hypixel resource API responses into the database. Subclasses: `ResourceItemsProcessor`, `ResourceSkillsProcessor`, `ResourceCollectionsProcessor`.

**`util/`** - Shared utilities:
- `SkyBlockUserCommand` - Abstract base for player-lookup commands; provides common parameters (`name`, `profile`), embed builders, and skill embed formatting
- `SkyBlockUser` - Resolves a Minecraft player from command arguments or linked Discord account; fetches profiles, guild, session, and auctions in the constructor
- `ItemCache` - Caches auction house, bazaar, and ended auctions from Hypixel API with expiry-based refresh

### Key Architectural Patterns

- **`SkyBlockUserCommand` template method**: `process()` is final and creates a `SkyBlockUser`, then calls abstract `subprocess()`. All player-facing commands follow this.
- **`ProfileStats` as computation hub**: Constructed from a `SkyBlockIsland` + `SkyBlockMember`, it loads every stat source, applies bonus calculations, and provides `getCombinedStats()` for total stat aggregation. The optimizer consumes this directly.
- **Optimizer flow**: `OptimizerRequest.of(username)` (builder) -> `Optimizer.solve(request)` -> `OptimizerResponse` containing solution, reforge counts, and final damage. The `Solution` base class handles reforge pruning (dominance elimination) and stat aggregation.
- **Two JPA entity sets**: `model/` entities are bot-application data (users, guilds, settings). `data/skyblock/` entities are game data (bonus stats, shop data). Both live in MariaDB and are accessed via `MinecraftApi.getRepository(ModelClass.class)`.

### Optimizer Math (README Summary)

**Damage Per Hit**: Maximizes `(100 + critDamage) * (100 + strength)` by allocating reforges. The quadratic expression is decomposed into const + linear + quadratic terms. OptaPlanner assigns reforges to items using incremental solving with a `.join` function and a `HundredReforgeProblemFact` to represent `100 + base_stat`.

**Damage Per Second**: Extends DPH with attack speed (`fer`, `as`) and crit chance (`cc`) variables: `averageDamagePerHit * hitsPerSecond`. Too many variables for CPLEX, so it relies on OptaPlanner's heuristic search.
