# S2 smoke test (~30 min): the Shipyard screen

**Setup**: `./build.sh` on the S2 branch, enable **Crew Stations** and **Console Commands** (plus LazyLib).
Any save works. Afterwards run `python3 tools/log_scan.py --mod .` and send it with your notes. Mod log lines
start with `[CrewStations]`; every build, upgrade and dismantle logs one line.

Handy console commands: `addcredits 10000000`, `additem heavy_machinery 2000` (also `metals`,
`rare_metals`), `goto <market>`, `storage`, `list markets`.

## 1. Where the option appears
- [ ] Dock at a market with **Heavy Industry** (e.g. a Hegemony or Persean League world): market menu shows
      **Visit the shipyard's crew Station works**.
- [ ] Dock at a market **without** heavy industry: no option.
- [ ] Market with **Orbital Works** (e.g. Chicomoztoc) or a nanoforge: footer says "up to level 3".
      Plain Heavy Industry: "up to level 2", and Upgrade to L3 is disabled with the reason.
- [ ] Transponder off / hostile market: no option (you cannot trade openly there).
- [ ] Your own colony with Heavy Industry: option shown.

## 2. Screen layout
- [ ] Opens a large panel: title with the market name, Close button, fleet list on the left with
      `used/slots` per ship (`automated` for automated ships), the flagship selected (`>`).
- [ ] Right side: ship picture, name, slots, skeleton crew line; one card per slot.
- [ ] Clicking another ship in the list switches the right side. Automated ship: "no crew, so no Stations".
- [ ] ESC: closes the confirmation, then the picker, then the screen. Close button closes it. The market menu
      is intact afterwards.

## 3. Build
- [ ] Empty slot → **+ Build** → grid of 10 Stations with icon, name and L1 effects. Ineligible ones (no
      shields, no bays, no missile slot...) have a disabled Build button and a red reason. Already built:
      "already built". Eligible ones show "Cost: ..."; with too few credits or materials the Build button is
      disabled and says what is short. **Back** returns.
- [ ] Build → confirmation bar: credits, each commodity split "cargo / storage / bought N for X", total vs your
      credits. Confirm disabled when something is short (red line) or credits are too low.
- [ ] Confirm → Station appears on its card, credits and materials go down by exactly the shown amounts.
      Refit tooltip shows it.
- [ ] A ship renamed with a `%` in its name (e.g. "100% Pure"): Build, Upgrade and Dismantle bars open fine.
- [ ] Materials split: put part of the heavy machinery in storage at this market (`storage`), part in cargo,
      none of the rest: the bar shows cargo + storage + bought from the open market; after confirm, storage
      and the open market stock went down too.

## 4. Upgrade / dismantle
- [ ] Upgrade to L2 shows that Level's cost; confirm → card shows Level 2 `[##-]` and the L2 effects.
- [ ] Dismantle → bar shows the refund (25% of credits paid, no materials); confirm → card empty, credits up,
      hullmod gone if it was the last Station.
- [ ] Inactive Station (e.g. Shield with Shield Shunt): card shows red "Inactive", Upgrade disabled.
- [ ] Locked storage: materials in storage you have not paid to unlock are not used.
- [ ] 20+ ships in the fleet: clicking a ship low in the list keeps the list scrolled where it was.

## 5. Report
- [ ] `python3 tools/log_scan.py --mod .` output + screenshots of anything that looks off (layout, overflow,
      overlapping text).
