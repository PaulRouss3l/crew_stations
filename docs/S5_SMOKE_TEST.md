# S5 smoke test (~25 min): locked Stations

**Setup**: `./build.sh` on the S5 branch, Crew Stations + Console Commands. Mod log lines start with
`[CrewStations]`. Afterwards run `python3 tools/log_scan.py --mod .`. `crst unlock` lists every locked
Station and whether it is offered; `crst unlock on|off|auto` forces them all offered / all locked / normal
until the game closes.

## 1. Greyed in The Catalogue
- [ ] New save, or `crst unlock off`: The Catalogue shows **ALL 11**, six columns. Missile Silo, Phase Station,
      Engine Station and Simulator Bay are greyed: dim art, status *Locked*.
- [ ] Click a greyed tile: detail panel shows UNLOCK in red with how to unlock it (*Destroy an enemy capital
      ship*, *Hold 1,000,000 credits*, *Carry 5,000 fuel*, *Win a Slay the Sector Run*); Order level 1 disabled with
      *Locked: ...*. `crst add missile 1` refuses ("not offered yet").
- [ ] Slay the Sector disabled in the launcher: the Simulator Bay says *Needs the Slay the Sector mod*.
- [ ] A ship that already had an Engine Station (0.1.x save): it stays, works, can be upgraded.

## 2. Unlocks and messages (`crst unlock auto`)
- [ ] `addcredits 1000000` (until you hold 1M), wait a day: notification *Station Yard Inc.: Phase Station
      available*; Intel entry under the *Station Yard Inc.* tag with the art, the letter and the signature. It
      fades out after 3 days. Phase Station now offered (UNLOCK green in the detail panel).
- [ ] `addfuel 5000` (fuel capacity permitting, or `addship` a tanker first), wait a day: Engine Station message.
- [ ] Win a fight that destroys or disables an enemy capital ship: Missile Silo message right after the battle.
- [ ] Spending the credits / fuel afterwards does not lock them again. Save, reload: still unlocked, no
      repeated message.
- [ ] With a Slay the Sector win: Simulator Bay message once per save (the day after loading).

## 3. Simulator Bay
- [ ] Order it (normal price): effects L1/L2/L3, *Any crewed ship*, UNLOCK green.
- [ ] Refit: hullmod tooltip lists *Maximum combat readiness +5%* and *Combat readiness recovery +15%*; max CR
      is 5 points higher (e.g. 70% -> 75%).
