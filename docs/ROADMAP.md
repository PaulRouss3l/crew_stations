# ROADMAP — Crew Stations

Each milestone is testable in game and ends with `./build.sh --strict` at 0 errors plus a
smoke-test list for the human tester. Design: `DESIGN.md`. Terms: `GLOSSARY.md`.

**Keep this file current**: when a feature lands, tick it and update the milestone status line.

Status legend: ⬜ planned · 🟡 code-complete, needs in-game test · ✅ verified in game.

**Current target: S1** — core: Stations on ships, effects, crew cost.

## S0 — Scaffold

- 🟡 Build/validate/run/release harness copied from Slay the Sector; tools vendored in `tools/`.
- 🟡 Hello world: `[CrewStations] application loaded` (launch) and
  `[CrewStations] game loaded (newGame=…)` (new game / load) appear in `starsector.log`.

## S1 — Core (no screen)

In-game checklist: `S1_SMOKE_TEST.md`.

- 🟡 `StationType` enum (10 types) with requirement checks (shields, phase, bays, weapon slot types).
- 🟡 Station data read/write as a variant tag, stock variants cloned first, modules included (ADR 0001).
- 🟡 Settings: every number from `data/config/settings.json` (`crst_` keys).
- 🟡 `crst_crew_stations` hullmod: built-in, 0 OP, applies all active Station effects + crew cost,
  tooltip lists Stations (inactive greyed).
- 🟡 Automated ships rejected everywhere.
- 🟡 Console commands: `crst add <station> <level>`, `crst clear`, `crst list` (flagship).
- ⬜ In game (`S1_SMOKE_TEST.md`): Stations survive save/load, storage, transfer, recovery; destroyed ships lose them.

## S2 — Shipyard screen

- ⬜ Shipyard detection (functional `heavyindustry`-tagged industry, not disrupted, owner not hostile).
- ⬜ L3 gate (Orbital Works or nanoforge; setting to disable).
- ⬜ Dock dialog option (rules.csv) at Shipyards only.
- ⬜ Screen: fleet list, ship Station cards, Upgrade/Dismantle, "+ Build" card grid with greyed reasons.
- ⬜ Costs: credits × hull multiplier; commodities from cargo → local storage → market stock.
- ⬜ Confirmation dialogs; dismantle refund 25% credits.

## S3 — Art

- ⬜ 10 Station card illustrations (ComfyUI + cleanup script), tone-matched to vanilla.
- ⬜ 10 Station icons + hullmod icon (PIL glyphs).
- ⬜ Screen banner.
- ⬜ Wired into the screen and the hullmod tooltip.

## S4 — Polish + release

- ⬜ Balance pass against vanilla s-mods/skills.
- ⬜ README, forum post, known issues (sold ship keeps Stations).
- ⬜ Release 0.1.0.
