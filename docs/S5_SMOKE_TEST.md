# S5 smoke test (~15 min): the Simulator Bay

**Setup**: `./build.sh` on the S5 branch, Crew Stations + Console Commands. Mod log lines start with
`[CrewStations]`. Afterwards run `python3 tools/log_scan.py --mod .`.

## 1. Locked
- [ ] With no Slay the Sector win (rename `saves/common/slay_the_sector_meta.json*` out of the way, or a
      fresh install): The Catalogue shows **ALL 10**, no *Reward* tab, five columns. `crst add simulator 1`
      refuses ("not offered yet"). `crst unlock` reports *found: no, offered: no*.

## 2. Unlocked
- [ ] With a Slay the Sector win in that file (or `crst unlock on`, or `"crst_unlockAll": true` in settings.json): the log shows
      `Slay the Sector win found: Simulator Bay offered` once. The Catalogue shows **ALL 11**, a *REWARD* tab,
      six columns; tiles still readable.
- [ ] Simulator Bay detail: effects L1/L2/L3, *Any crewed ship*, and the green *Slay the Sector reward* line.
      Normal price. Order, sign: it appears on the ship card with its art.
- [ ] Refit: hullmod tooltip lists *Maximum combat readiness +5%* and *Combat readiness recovery +15%*; the
      ship's max CR is 5 points higher (e.g. 70% -> 75%).

## 3. Lock again
- [ ] Remove the unlock (file away, `crst unlock off`, `crst_unlockAll` false), reload: the built Simulator Bay stays and works;
      The Catalogue hides it for other ships, shows it for the ship that has it.
