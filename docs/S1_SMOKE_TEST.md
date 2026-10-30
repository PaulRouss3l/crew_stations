# S1 smoke test (~20 min): Stations on ships, no screen yet

**Setup**: `./build.sh`, enable **Crew Stations** and **Console Commands** (plus LazyLib) in the launcher.
Any save works. Open the console with Ctrl+Backspace on the campaign map.
Afterwards run `python3 tools/log_scan.py --mod .` and send it with your notes. Mod log lines start with
`[CrewStations]`. Tunables are in `data/config/settings.json` (`crst_*`).

## 1. Launch
- [x] The game reaches the main menu; log shows `[CrewStations] application loaded`.
- [x] Load or start a game; log shows `[CrewStations] game loaded (newGame=…)`.
- [x] Codex → Hullmods: **Crew Stations** is not listed as installable (it is hidden), or shows only the
      "built at a Shipyard" line.

## 2. Console basics (flagship, or the ship picked with `crst select <n>`)
- [ ] `crst ships` → numbered fleet list with slots used, `*` on the selected ship. `crst select 2` → that
      ship; `crst select flagship` → back to the flagship.
- [x] `crst list` → `<ship> (<hull>): 0/<slots> slots, crew +0%`. Slots: frigate 1, destroyer 2, cruiser 3, capital 4.
- [x] `crst add engine 1` → list shows `engine L1`, crew +10%.
- [x] `crst add engine 3` → "upgrade one level at a time (now level 1)". `crst add engine 2`, then
      `crst add engine 3` → `engine L3`, crew +30%. `crst add engine 1` now → refused (no downgrade).
- [x] `crst add logistics 2` on a ship without it → "build at level 1 first".
- [x] `crst add foo 1` → "Unknown station", lists the ids.
- [x] `crst add reactor 4` → "level must be 1 to 3".
- [x] Fill every slot, then add one more type → "no free Station slot".
- [x] On a shieldless ship: `crst add shield 1` → "needs shields". Same for phase / flight_deck / weapon types
      where the ship lacks them.

## 3. Refit
- [x] Refit screen: the flagship shows a **Crew Stations** hullmod (cannot be removed, 0 OP, no S-mod option).
- [x] Its tooltip: heading "Stations (n of m slots)", each Station with Level name and bullet effects, a red
      "Skeleton and max crew +x%" line.
- [x] The ship's stats moved as expected, e.g. Engine L3: top speed +15%; Reactor L2: flux capacity and
      dissipation +10%; skeleton crew +x% in the ship's crew line.
- [x] Another ship of the **same hull** (buy or `addship`) does NOT have the Stations or the hullmod.
- [x] Remove the shields (Shield Shunt) on a ship with a Shield Station: tooltip shows it greyed
      "inactive, needs shields"; crew cost unchanged. Remove the Shunt: active again.

## 4. Lifecycle
- [x] `crst clear` → hullmod gone from refit, crew +0%.
- [x] Add Stations, save, quit to menu, load: Stations and hullmod still there.
- [x] Put the ship in storage, take it back; transfer it as non-flagship and back: Stations kept.
- [x] Mothball and reactivate: Stations kept.
- [ ] Let the ship be disabled in a fight and recover it: Stations kept.
- [x] Two ships of the same **multi-module** hull (if you have one): Stations on one do not appear on the other.
- [x] Refit → save the Station ship's variant as a goal variant; nothing errors (NPC fleets may at worst carry
      an inert hullmod).
- [ ] Automated ship: `addship rampart`, then `crst ships` and `crst select <n>` on it (the player cannot
      pilot it, so it cannot be the flagship). `crst add engine 1` → "automated ships have no crew for Stations".
      `crst ships` shows it as `automated`.

## 5. Combat
- [ ] Simulator or a real fight with a Station ship: no errors. Missile Silo L3: missile weapons show double ammo.
      Flight Deck: fighters a bit faster.

## 6. Report
- [ ] `python3 tools/log_scan.py --mod .` output + anything that looked wrong.
