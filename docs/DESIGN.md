# DESIGN — Crew Stations

Settled design (grilling session, 2026-10-30). Terms: `../GLOSSARY.md`. Milestones: `ROADMAP.md`.
Storage decision: `adr/0001-station-data-on-variant-tag.md`.

## Concept

The player improves crewed ships by building **Stations** aboard them: dedicated crew compartments
(shield room, engine bay, missile silo...) that give permanent stat bonuses. Stations are built and
upgraded at a **Shipyard** (a market with heavy industry) through a visual screen, cost a lot of credits and
materials, and need extra crew. Automated ships cannot have them.

## Scope

- **Player ships only.** NPC fleets never get Stations.
- **Automated ships excluded** (`Misc.isAutomated(member)`: `automated` hullmod, variant tag or hull tag).
- **No dependencies.** Console Commands is optional (debug only).
- Safe to add to a running save. Removing the mod is not supported.

## Rules

### Slots, types, levels

- Slots per hull size: frigate 1, destroyer 2, cruiser 3, capital 4.
- At most one Station of each type per ship.
- Three Levels: L1 Basic, L2 Advanced, L3 Elite. A Station is built at L1 and upgraded one Level at a
  time. No downgrade: the only way back is to dismantle the whole Station.

### Station types and effects (L1 / L2 / L3)

| Station | Requires | Effect A | Effect B |
|---|---|---|---|
| Shield | shields | shield damage taken −5/−10/−15% | shield turn + unfold rate +15/30/50%, upkeep −10/20/30% |
| Logistics | — | supplies/month −5/−10/−15% | fuel use −10/−20/−30% |
| Engine | — | top speed +5/10/15% | acceleration, deceleration, turn rate +15/30/50% |
| Energy | ≥1 energy-capable slot | energy weapon damage +3/6/10% | energy weapon flux cost −5/−10/−15% |
| Ballistic | ≥1 ballistic-capable slot | ballistic rate of fire +5/10/15% | ballistic flux cost −5/−10/−15% |
| Missile | ≥1 missile-capable slot | missile ammo +25/50/100% | missile HP +15/30/50%, missile reload +15/30/50% |
| Phase | phase cloak | cloak upkeep −10/−20/−30% | cloak cooldown −10/−20/−30% |
| Reactor | — | flux capacity +5/10/15% | flux dissipation +5/10/15% |
| Damage control | — | hull damage taken −5/−10/−15% | weapon/engine repair rate +25/50/75%, crew loss −25/50/75% |
| Flight deck | fighter bays | fighter replacement time −10/−20/−30% | fighter top speed +5/10/15% |

Hybrid, composite, synergy and universal slots count for the weapon Stations when they accept that type.
All values are v1 baselines, tunable in settings.

### Inactive Stations

A Station whose requirement is no longer met (e.g. the ship lost its shields in refit) stays aboard but
is **inactive**: no effect, shown greyed in the screen and the hullmod tooltip. It still costs crew.

### Crew cost

Each Station Level adds +10% to the ship's skeleton crew and +10% to its max crew (so the extra crew fits).
Inactive Stations count. Understaffing uses the vanilla CR penalty; nothing custom.

### Multi-module ships

Effects apply to the parent ship and all its modules. Slots and prices follow the parent's hull size.

### Lifecycle

Stations belong to the ship: kept through storage, mothballing, fleet transfers and disabled-then-recovered
ships. Lost when the ship is permanently destroyed. A sold ship keeps its Stations on its variant, so
buying it back restores them (accepted, documented for players).

## Cost

Credits per Level = Level base × hull multiplier. Each upgrade pays only that Level's price.

| | L1 | L2 | L3 |
|---|---|---|---|
| Credits base | 150,000 | 400,000 | 900,000 |
| Commodities base | 100 heavy machinery | 200 heavy machinery + 500 metals | 300 heavy machinery + 200 rare metals |

Hull multiplier (credits and commodities): frigate ×0.5, destroyer ×1, cruiser ×2, capital ×4.
A capital L3 costs 3.6M credits + 1,200 heavy machinery + 800 rare metals. Stations are a late-game
material project on purpose.

**Paying commodities**, in order: fleet cargo → the player's storage at that market → buy the rest
from the market's stock at its price. If all three fall short, the action is disabled and the tooltip
names what is missing.

**Dismantle**: refunds 25% of all credits paid for that Station's Levels. No commodities back.

Build, upgrade and dismantle all show a confirmation with the full cost or refund. All are instant.

## Shipyards

A market is a Shipyard when it has a functional industry tagged `heavyindustry` (vanilla Heavy Industry
and Orbital Works, plus modded ones using the tag, e.g. HMI's Church of Eva Complex), is not disrupted,
and its owner is not hostile to the player. Player colonies count.

- L1–L2: any Shipyard.
- L3: Orbital Works, or a heavy-industry industry with a nanoforge installed (pristine or corrupted).
  Gate can be switched off in settings.

## Screen

A "Crew Stations" option appears in the market dock dialog (rules.csv `PopulateOptions`) only at a
Shipyard. It opens a full-screen custom dialog:

```
┌────────────────────── Crew Stations — <market> ───────────────── [X] ┐
│ FLEET            │  [ship sprite]  Onslaught (Capital)  slots 2/4      │
│ ▸ Onslaught  2/4 │  Skeleton crew 500 → 700 (+40%)                      │
│   Hammerhead 1/2 │ ┌──────────┐┌──────────┐┌──────────┐┌──────────┐    │
│   Wolf       0/1 │ │[art]     ││[art]     ││  + Build ││  + Build │    │
│   (Rampart)  n/a │ │ Ballistic││ Reactor  ││  station ││  station │    │
│   automated grey │ │ ●●○  L2  ││ ●○○  L1  ││          ││          │    │
│                  │ │[Upgrade] ││[Upgrade] ││          ││          │    │
│                  │ │[Dismantle]│[Dismantle]│          ││          │    │
│                  │ └──────────┘└──────────┘└──────────┘└──────────┘    │
│                  │ Cost: 900,000¢  300 HM  200 rare metals  [have ok] │
│ Credits 4.2M     │ Shipyard cap: L3 (Orbital Works)                     │
└──────────────────────────────────────────────────────────────────────┘
```

- Left: player fleet; automated ships listed greyed as "n/a".
- Right: selected ship, its slots as Station cards (art, name, Level pips, effects, Upgrade/Dismantle),
  empty slots as "+ Build".
- "+ Build" opens a card grid of all Station types: eligible ones show art, L1 effects and price;
  ineligible ones greyed with the reason.
- Starting point for the code: Slay the Sector's `CardGridDelegate` (paged cards, rebuild-on-change).

## Hullmod

`crst_crew_stations`: visible, built-in (perma-mod), 0 OP, cannot be removed. Added to a ship's variant
when it gets its first Station, removed when its last Station is dismantled. Applies every active
Station's effects (and the crew cost) in `applyEffectsBeforeShipCreation`; its tooltip lists each Station
with Level and effects, inactive ones greyed.

## Art

- 10 Station card illustrations (~128×128): AI-generated with the local ComfyUI (Z-Image Turbo), then
  cropped, downscaled and tone-matched to vanilla by script. Interior shots of the crewed compartment,
  worn industrial sci-fi, muted palette, one accent colour per type (shield cyan, engine orange,
  energy violet...).
- 10 Station icons (32×32) and the hullmod icon: script-drawn glyphs (PIL) in vanilla's flat icon style.
- 1 screen banner (AI-generated).

## Configuration

Every number lives in `data/config/settings.json` under `crst_` keys and is read at application load;
changes apply to existing Stations too. No LunaLib. Covers: effect values per type and Level, crew %
per Level, slots per hull size, credit bases, commodity amounts per Level, hull multipliers, refund
fraction, L3 gate on/off.

## Text

English only in v1. Player-facing text lives in `data/strings/` and `data/strings/descriptions.csv`,
never hard-coded.

## Debug

Console Commands (optional): `crst add <station> <level>`, `crst clear`, `crst list`, acting on the
player's flagship.

## Planned packages

| Package | Holds |
|---|---|
| `com.crewstations` | mod plugin |
| `com.crewstations.station` | `StationType`, Station data, eligibility, read/write on the variant |
| `com.crewstations.hullmods` | the Crew Stations hullmod |
| `com.crewstations.shipyard` | Shipyard detection, costs, payment |
| `com.crewstations.ui` | the screen |
| `com.crewstations.debug` | console commands |
| `com.crewstations.util` | `CSLog`, settings |
