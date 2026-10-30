# 0001 — Station data lives in a tag on the ship's variant

**Status**: accepted (2026-10-30)

## Context

Each player ship carries its own Stations (type + Level). The data must survive save/load, storage,
mothballing, fleet transfers and post-battle recovery, and stay XStream-safe. Options: a map in sector
persistent data keyed by `FleetMemberAPI.getId()`, or data carried on the ship's variant.

Exotica Technologies (installed, source included) moved from the persistent-data map to a variant tag:
it serialises its per-ship data to JSON in a tag (`"$$EXOTICA$$" + json`), first cloning stock variants
into a REFIT-source variant so the tag sticks to that one ship, and applies effects through a hullmod
added with `addPermaMod`.

## Decision

Store each ship's Stations as one variant tag `crst_stations:<json>` on the ship's own (REFIT-source)
variant, and on every module variant. Before writing, clone a stock or non-REFIT variant into a REFIT
one so other ships of the same hull are unaffected. The `crst_crew_stations` hullmod reads the tag.

## Consequences

- Follows the ship everywhere with no lifecycle code: storage, transfer, recovery all work.
- Selling a ship and buying it back restores its Stations (accepted, documented).
- No persistent-data map to clean up; no saved classes to alias (the tag is a plain string).
- The tag format is a save format: never rename the tag prefix or JSON keys after release; add fields
  only with defaults.
- Destroyed ships need explicit handling only if a vanilla path ever keeps their variant; checked in S1.
