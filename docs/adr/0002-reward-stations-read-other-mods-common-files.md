# 0002 — Reward Stations read other mods' cross-save files

**Status**: accepted (2026-10-30)

## Context

The Simulator Bay is a reward for winning a Run in Slay the Sector (sister mod, same author). The two mods
must stay independent: each installs, loads and updates without the other. Slay the Sector already keeps
its cross-save progress in `saves/common/slay_the_sector_meta.json` (`archetypes.<id>.wins`,
`winArchetypes`).

## Decision

Crew Stations reads that file with `SettingsAPI.readJSONFromCommon`, at game load and when the showroom
opens, and caches one boolean. No class, jar or `dependencies` entry ties the mods together. Slay the
Sector, for its part, only mentions the unlock in its Run-end report when Crew Stations is enabled.

## Consequences

- No load-order or version coupling; either mod can be removed (removing Crew Stations still has the
  usual save caveat).
- The unlock is cross-save, like Slay the Sector's own Records.
- Coupled to two JSON keys of another mod's file. If Slay the Sector renames them, the Simulator Bay
  stops being offered (already-built ones stay). Both sides document the keys.
- The file outlives the mod, so a win only counts while Slay the Sector is enabled.
- A broken or missing file reads as "not won". `crst_unlockAll` in settings offers it anyway (testing).
