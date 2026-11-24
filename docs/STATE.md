# GTNA — current state

Short, current snapshot for anyone (person or AI session) starting work on GTNA. Keep it under ~150 lines: replace outdated lines instead of appending. History lives in git commits and `CHANGELOG.md`; the old `CONTINUITY_LEDGER.md` was retired on 2026-10-16 and is still in git history (last commit with it: `75269ab`).

Updated: 2026-10-16 · version **0.6.0** (+ unreleased fixes on `main`) · Minecraft 1.20.1, Forge 47, GTCEu 7.5.3, requires **TypeOmega Lib 0.5.0+**.

## What exists

- **170 multiblocks.** Ports from GTO, GTL, GTNL and GTNH plus GTNA originals. Progress of the requested ports: [roadmap](roadmap/index.md) (46/86, by phase A–G). Only machines from the author's list are ported, from the exact source mod ([lista-do-autor](roadmap/lista-do-autor.md)).
- **Endgame:** Eye of Harmony, Forge of Gods (Godforge chain), Nano Forge T1–T4, Bose-Einstein Condensate network, Plasma Forge, Space Elevator with modules, Dyson Sphere.
- **Originals:** Kinetic Orbital Lance, Stellar Containment Chamber, Antimatter Collider Ring and Dimensional Rift Gate are in game and under test; the Pressurized Water Reactor (IV) is designed.
- **Systems:** Nexus energy network (128-bit wireless energy, configurable losses), wireless steam network (both keyed by the effective FTB team, per player without FTB Teams; legacy per-player networks merge into the team lazily, `TeamNetworkMigration`), ME Pattern Buffers (multi-mode, configurable capacity), module system (sub-patterns that unlock capacity or recipe types), Thread/Parallel/Overclock hatches, electric Void Miner and void fluid drilling, Steam Elevator with modules, research engine (used by GTIA only if enabled).
- **Item effects:** own model loaders (`gtna:halo`, `glow_edge`, `cosmic`, `eternal`, `unstable`); RenderBlender is no longer needed.
- **Tests:** about 390 `@GameTest` methods plus unit tests (`runUnitTests`).

## Validation gate

```bash
./gradlew gate --offline
```

- `gate` runs spotlessCheck, compileJava, runUnitTests, runGameTestServer and runData. `verifyGameTestLog` runs right after the GameTests and fails the build on `Parsing error loading recipe gtna:` or a failed batch. No manual grep is needed.
- After adding lang keys, run `./gradlew runData` once first: `AssetLangCoverageTest` reads the generated en_us.json, and the gate runs runData last.
- Running `runData` in the same command disables runtime dependencies (`isDataRun`), including FTB Teams and FTB Quests; for the FTB GameTests run `./gradlew runGameTestServer` on its own.
- Create `run/eula.txt` with `eula=true` before the first GameTest run.

## Rules that cost us before

- **Never name a JEI type outside JEI-only classes** (`integration/jei/`, mixins, `JeiPreviewOpen`): EMI-only clients fail to load the whole mod. `JeiIsolationContractTest` enforces it.
- **Never reference `net.minecraft.client.*` from common code**, not even inside a lambda: the dedicated server rejects the class and the whole mod fails to load. Use an `@OnlyIn(Dist.CLIENT)` class reached through `DistExecutor`.
- **UI layout needs one source of truth for geometry and a test for it** (pattern: `PatternBufferLayout` + `PatternBufferLayoutTest`). LDLib only checks `isVisible()`, so a widget placed outside the page is still drawn over the frame.
- **Own hatches need `modelProperty(IS_FORMED)`**, or GTCEu never swaps the hull for the casing texture.
- **Output insertion does a dry run on a copy of the slots**, so a partial insert never duplicates items.
- **Energy and fluid spending is SIMULATE → charge → EXECUTE**, never a partial payment; balances never go negative.
- Module orientation rules: see [NEXT-SESSION-HANDOFF](roadmap/NEXT-SESSION-HANDOFF.md).

## Conventions

- Tooltips: moving family by family to the GTNH layout of the lib (`MultiblockTooltip.builder()`, GTIA-D-410; no Hold [LSHIFT] line, D-428). Migrated families live in `common/data/tooltip/` with their texts in `assets/gtna/tooltip_lang/<family>.json` (`[en, pt, zh]`; `python3 tools/tooltip_lang.py` copies pt/zh into the lang files). Done: large steam (19), GTNL steam utilities (9), Infernal Coke Oven and Hyper Pressure Reactors (3). Full mechanics from the original (GTIA-D-432, `MechanicTooltipGameTests` keeps a minimum line count per machine): Eye of Harmony, Mega Alloy Blast Smelter, Forge of Gods and its four modules. The rest still use the generated layout (`tools/multiblock_tooltips/`, [tooltip-standard](roadmap/tooltip-standard.md)). Every tooltip credits a port's origin with a `Source:` line (`GTNASources`).
- Ports prefer GTNL recipe types; GTO recipe types only for the approved exceptions.
- Every name in en_us, pt_br and zh_cn; changelogs in English.
- Code GTNA writes is LGPL-3.0; GTO assets are CC BY-NC-SA 4.0. Attribution in `THIRD_PARTY_NOTICES.md`.
- Organisation code rules (file size, trilingual key comments): [conventions](https://type-omega.github.io/TypeOmega-Dev-Doc/development/conventions/).

## Large files to split when touched

| File | Lines |
| --- | --- |
| `gametest/GTNAMachineGameTests.java` | ~7,700 |
| `common/data/AnnihilateGeneratorA.java` | ~6,300 |
| `common/data/AnnihilateGeneratorB.java` | ~6,000 |
| `common/data/GTNAMachines.java` | ~3,800 |
| `common/data/multiblock/GTNAGTOPatterns.java` | ~2,500 |
| `data/recipe/GTNAMachineRecipes.java` | ~2,400 |
| `data/GTNALangProvider.java` | ~2,400 |

## Open items

- Next release (0.6.1) must say: requires TypeOmega Lib; fixes the client crash without JEI and 10 recipes that failed to load; single-block cover check 8.37 → 3.74 ms/tick with 2,000 furnaces; soft preview shape cache.

- DTPF Convergence and the ISA Mill tiers (roadmap phases A and B).
- Phases D–G of the port list (large GTO systems, GTNH-only, TST/GTNL/123Tech, GTLAdditions).
- In-game QA of the four originals (they are marked WIP).
- Pressurized Water Reactor implementation.
