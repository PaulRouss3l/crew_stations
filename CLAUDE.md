# CLAUDE.md — Crew Stations

Context router for AI sessions. Keep this file lean; detail lives in `docs/`.

## What this is

A **Starsector 0.98a** mod: players build and upgrade crewed **Stations** aboard their ships at
heavy-industry **Shipyards** for permanent stat bonuses (automated ships excluded). **Java only**
(no Kotlin). Design: `docs/DESIGN.md`; storage: `docs/adr/0001-*`.
Domain terms: `GLOSSARY.md`.

## Build / run

- Build: `./build.sh` → compiles `src/` into `jars/CrewStations.jar`, then runs the static
  validator. Must report **0 errors** after every change. `./build.sh --strict` before a release.
- Run (debug, attach on `:5005`): `./run.sh`. In VSCode: F5 "Debug Starsector".
- After an in-game test: `python3 tools/log_scan.py --mod .` triages `starsector.log`.
  Mod lines start with `[CrewStations]`.
- Release: bump the version (`mod_info.json`, `crew_stations.version` incl. `directDownloadURL`,
  `CHANGELOG.md`), merge to `main`, then `./release.sh X.Y.Z [--prerelease]` (`--dry-run` first).
  Steps: `docs/DEVELOPMENT.md`.
- Game install: `/home/ubik/Downloads/starsector` (override with `STARSECTOR_DIR`). The repo is
  symlinked into `<install>/mods/crew-stations` (see `docs/DEVELOPMENT.md`).
- Entry class: `com.crewstations.CrewStationsModPlugin` (per `mod_info.json`).

## Tooling

- `tools/` is **vendored** from the `starsector-modding` skill (see `tools/UPSTREAM.md`). Do not
  edit those scripts; configure via `tools/validate_config.json`.
- The full skill is installed at `~/.claude/skills/starsector-modding`. Use its
  `scripts/api_lookup.py` (`class|method|const|uses|grep|rulecmd|javap`) to check any API call
  before using it, and its `references/pitfalls.md` as the review checklist.

## Hard constraints

- **Java only.** Do not introduce Kotlin.
- **Id prefix `crst_`** on every new id (hulls, weapons, factions, entities, rules, memory keys,
  settings keys). The validator enforces it.
- **Never wipe / regenerate the vanilla sector.** The mod is purely additive.
- **Never relocate the player fleet from inside an open dialog** (camera gets stuck).
- **XStream-safe serialization**: saved state holds ids and plain `Serializable` POJOs, not
  references to live world objects. No static fields holding game objects. Alias saved classes in
  `configureXStream`; never rename them after release.

## Workflow

- **When a feature is done, update `docs/ROADMAP.md`** in the same change: tick the item
  (⬜ → 🟡 code-complete, ✅ verified in game) and the "Current target" line.
- New domain terms go in `GLOSSARY.md`; hard-to-reverse design choices get an ADR in `docs/adr/`.
- **Logging** (`CSLog`, ships to players' `starsector.log`): log events, never loops. Info lines for
  rare milestones only; `warn` for real anomalies. Nothing in `advance()`, rendering or per-tick
  code. Verbose tracing, if ever needed, goes behind a `crst_debugLog` setting that defaults to off.

## Pointers

- `GLOSSARY.md` — canonical domain terms.
- `docs/DESIGN.md` — settled design.
- `docs/adr/` — architecture decisions.
- `docs/ROADMAP.md` — milestones and status.
- `docs/API_NOTES.md` — verified Starsector API facts + pitfalls relevant to this mod.

## Reference mods

- Sister project **Slay the Sector** at `/home/ubik/localhost/perso/starsector/slay-the-sector`
  (same harness; mature examples of intel, dialogs, custom UI, battle plugins, console commands).
- API docs mirror: `https://jaghaimo.github.io/starsector-api/`.
- Nexerelin (`Histidine91/Nexerelin`), SecondInCommand / RAT (`Lukas22041`) — examples.
