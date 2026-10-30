# Changelog

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
