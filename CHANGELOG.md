# Changelog

## 0.2.0 (beta)

- **Simulator Bay**, a new reward Station: crews trained in Slay the Sector's Descent Relay simulation. Max
  combat readiness +5/10/15, CR recovery +15/30/50%. Station Yard Inc. offers it once you have won a Run in
  Slay the Sector (any save, any faction); it then appears in The Catalogue under *Reward*. Neither mod
  needs the other.

## 0.1.1 (beta)

- **Station Yard Inc.:** the shop is now run by a luxurious megacorp. The dock menu offers *Visit the Station
  Yard Inc. showroom*; the screen has a slogan of the day, the picker is *The Catalogue*, orders are signed
  and returns come with a restocking fee. Station descriptions and the hullmod text got the same treatment.
- Levels are now **Standard, Deluxe and Platinum** (were Basic, Advanced, Elite).


## 0.1.0 (beta)

First public release.

- **Stations:** ten crewed Station types (Shield, Logistics, Engine, Energy, Ballistic, Missile Silo, Phase,
  Reactor, Damage Control, Flight Deck), three levels each. Slots per hull: frigate 1, destroyer 2,
  cruiser 3, capital 4. Each level adds 10% skeleton and max crew. Automated ships cannot have Stations.
- **Requirements:** shields, phase cloak, fighter bays or the right weapon slot. A Station whose
  requirement is no longer met goes inactive.
- **Shipyards:** any market with working Heavy Industry whose owner is not hostile. Level 3 needs
  Orbital Works or a nanoforge.
- **The crew Station works screen:** fleet list, ship panel, Station cards, a picker with every effect at
  every level, and a confirmation drawer showing exactly what you pay and where the materials come from
  (cargo, storage, open market). Dismantle refunds 25% of the credits.
- **Costs:** credits plus heavy machinery, metals and rare metals, scaled by hull size.
- **Crew Stations hullmod:** applies the effects and lists every Station in the refit screen.
- **Art:** ten Station illustrations and the hullmod icon.
- **Settings:** every number in `data/config/settings.json` (`crst_*`).
- **Console Commands:** `crst ships|select|list|add|clear` for testing.

## 0.0.1 (dev)

- Project scaffold: build, validate, run, release tooling. Logs a hello-world line on load.
