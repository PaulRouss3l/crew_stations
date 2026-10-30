# Crew Stations

Canonical domain terms. Add a term when it first appears in code or design; list synonyms to avoid.

## Language

**Station**:
A crewed compartment built aboard one ship, of one Station type at one Level, giving permanent stat bonuses.
_Avoid_: module (a Starsector ship module is a different thing), upgrade (that is the action), crew station
(the mod's name, not one Station)

**Station type**:
One of the ten kinds of Station: Shield, Logistics, Engine, Energy, Ballistic, Missile, Phase, Reactor,
Damage control, Flight deck.

**Level**:
A Station's rank, L1 Standard, L2 Deluxe, L3 Platinum. Raised one at a time by an Upgrade.
_Avoid_: tier, rank

**Slot**:
Room for one Station on a ship; the count depends on hull size (1/2/3/4).
_Avoid_: weapon slot (unrelated vanilla term), bay

**Build / Upgrade / Dismantle**:
The three actions: create a Station at L1, raise its Level by one, remove it entirely for a partial refund.
_Avoid_: install, remove, downgrade (not supported)

**Inactive Station**:
A Station whose requirement the ship no longer meets (e.g. no shields after a refit); no effect, still costs crew.

**Shipyard**:
A market where Stations can be built: functional `heavyindustry`-tagged industry, not disrupted, owner not hostile.
_Avoid_: dockyard, station (a space station is a market location, not a Station)

**Station Yard Inc.**:
The in-world megacorp that sells and fits Stations. Its **showroom** is the screen where Stations are built,
upgraded and dismantled, opened from a Shipyard's dock menu. Luxurious and a little smug.
_Avoid_: crew Station works (the 0.1.0 name), Station yard

**L3 gate**:
The rule that L3 needs a Shipyard with Orbital Works or a nanoforge.

**Crew Stations hullmod**:
The built-in hullmod (`crst_crew_stations`) that applies a ship's Stations; present only while the ship has one.
