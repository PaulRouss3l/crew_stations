# Crew Stations

![Crew Stations](docs/assets/banner.png)

**Build and upgrade crewed Stations aboard your ships for permanent bonuses.**

*Beta 0.1.0, for Starsector 0.98a.*

[![Install with TriOS](https://trilink.wispborne.com/badges/install-badge-forthebadge.svg)](https://trilink.wispborne.com/open.html?mod=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FPaulRouss3l%2Fcrew_stations%2Fmain%2Fcrew_stations.version%22%2C%22id%22%3A%22crew_stations%22%7D)

**[Download the latest release](https://github.com/PaulRouss3l/crew_stations/releases)** (zip, extract into `mods/`). No dependencies.

> *Tired of crews who merely survive? **Station Yard Inc.**, the Sector's most exclusive outfitter, fits
> your ships with hand-crafted crewed Stations, staffed by the finest specialists money can rent. Visit
> any of our showrooms, conveniently located wherever there is heavy industry. Prices are high because
> quality is high. Also because we can.*

Dock at any market with heavy industry and visit the **Station Yard Inc. showroom**. There you can
build dedicated, crewed compartments aboard your ships: a shield control room, an engine room, a
missile magazine... Each Station improves one part of the ship and can be upgraded twice. They are
expensive, they need extra crew, and they stay with the ship for good.

## How it plays

1. **Find a Shipyard.** Any market with working **Heavy Industry** (or a modded industry tagged as
   one) whose owner is not hostile, your own colonies included. The dock menu shows *Visit the
   Station Yard Inc. showroom*.
2. **Pick a ship.** Frigates have 1 Station slot, destroyers 2, cruisers 3, capitals 4. Automated
   ships have no crew, so no Stations.
3. **Order a Station** from the Catalogue. Ten types, each with its own requirement (shields, a phase cloak, fighter
   bays, a weapon slot of the right kind...). The picker shows every effect at every level, the
   price, and where the materials will come from.
4. **Upgrade it.** Level 1 Standard, Level 2 Deluxe, Level 3 Platinum. Platinum needs a showroom with
   **Orbital Works** or a nanoforge.
5. **Pay for it.** Credits plus heavy machinery, metals and rare metals, taken from your cargo first,
   then your storage at that market, then bought from its open market. Bigger hulls pay more.

Every Station level adds **10% to the ship's skeleton and max crew**. A Station whose requirement is
no longer met (for example shields turned into Shield Shunt) goes **inactive**: no bonus, crew still
needed. Dismantling refunds 25% of the credits paid (after Station Yard Inc.'s restocking fee), no
materials.

## The ten Stations

| Station | Needs | Level 1 / 2 / 3 |
|---|---|---|
| Shield Station | shields | shield damage taken −5/10/15%, turn and unfold rate +15/30/50%, upkeep −10/20/30% |
| Logistics Station | — | supplies per month −5/10/15%, fuel use −10/20/30% |
| Engine Station | — | top speed +5/10/15%, acceleration, deceleration and turn rate +15/30/50% |
| Energy Station | an energy slot | energy weapon damage +3/6/10%, energy flux cost −5/10/15% |
| Ballistic Station | a ballistic slot | ballistic rate of fire +5/10/15%, ballistic flux cost −5/10/15% |
| Missile Silo | a missile slot | missile ammo +25/50/100%, missile hitpoints +15/30/50%, reload rate +15/30/50% |
| Phase Station | a phase cloak | phase upkeep −10/20/30%, phase cooldown −10/20/30% |
| Reactor Station | — | flux capacity +5/10/15%, flux dissipation +5/10/15% |
| Damage Control Station | — | hull damage taken −5/10/15%, weapon and engine repairs +25/50/75%, crew lost −25/50/75% |
| Flight Deck | fighter bays | fighter replacement time −10/20/30%, fighter top speed +5/10/15% |

## Prices

For a destroyer. Frigates pay ×0.5, cruisers ×2, capitals ×4 (credits and materials).

| Level | Credits | Materials |
|---|---|---|
| 1 | 112,500 | 75 heavy machinery |
| 2 | 300,000 | 150 heavy machinery, 375 metals |
| 3 | 675,000 | 225 heavy machinery, 150 rare metals |

Every number (effects per level, slots, crew cost, prices, hull multipliers, refund, the level 3
rule) can be changed in `data/config/settings.json` (`crst_*` keys).

## Compatibility

- **Other mods:** works with modded ships and modded heavy industries. Tested alongside Nexerelin,
  Exotica Technologies and Random Assortment of Things.
- **No dependencies.** With [Console Commands](https://fractalsoftworks.com/forum/index.php?topic=4106.0)
  installed, the `crst` command lists, adds and clears Stations (handy for testing).
- **Existing saves:** safe to add to a game in progress.
- **Removing the mod:** not supported. Ships with Stations carry the Crew Stations hullmod, and a save
  that has them needs the mod to load. Keep a backup if you plan to remove it.

## Install

With [TriOS](https://github.com/wispborne/TriOS): click the **Install with TriOS** button above.

Manually:

1. Download the zip from the [Releases](https://github.com/PaulRouss3l/crew_stations/releases) page.
2. Extract it into `Starsector/mods/` (you get `mods/crew-stations/`).
3. Enable **Crew Stations** in the launcher.

Version Checker (bundled with Nexerelin and LunaLib) will tell you when an update is out.

## Beta notes

This is a beta: balance is a first pass and bugs are possible. Reports are welcome on the forum
thread or as GitHub issues. Please attach your `starsector.log`; the mod's lines start with
`[CrewStations]`.

Known issues:
- A ship sold to a market keeps its Stations if you buy it back.

## Credits

Thanks to the people whose work and open source code taught me how Starsector mods are made, and
whose mods I love playing:

- **wispborne**, for the Starsector IntelliJ Template, TriOS, and everything they do for the modding
  community. I learned to make my first mod with their tutorials.
- **Lukas22041**, for Second In Command and Random Assortment of Things, a great example of what a
  Starsector mod can be. I never play Starsector without Lukas's mods.
- **Histidine91**, for Nexerelin, a reference for nearly every campaign question.
- **The Exotica Technologies authors**, whose way of storing ship upgrades on the ship's variant this
  mod follows.
- **tracesfx**, for the starsector-modding skill: the build, validation and log tools, and the API
  notes this project relies on.

And to Alex and the Fractal Softworks team for Starsector <3.

## AI use disclaimer

Some or all assets and code in this mod were created using AI tools:

- **Code:** Claude Opus 5.5 (Anthropic), through Claude Code. It wrote most of the Java code, the
  build and art tooling, and the design and API documentation.
- **Code review:** Claude was also used to review each milestone for bugs before release. No line of
  code reaches a release without a human review.
- **Art:** the ten Station illustrations, the Crew Stations hullmod icon and the forum banner
  (`docs/assets/banner.png`) were made with Z-Image Turbo (Alibaba's Tongyi Lab), an open image model
  run locally in ComfyUI; the banner starts from the vanilla Onslaught sprite. Prompts and the
  pipeline: [`docs/ART.md`](docs/ART.md). The banner title uses the Orbitron font (SIL Open Font
  License).

How it was made: I designed the mod (the Stations, the Shipyard rules, the screen and the balance) and
directed every change. Each milestone was then played and tested in game by me, against a checklist,
before it was merged. The full source and the design notes are public in this repository.

## License

[MIT](LICENSE): use, change, fork or redistribute the mod freely, as long as the copyright notice and
the license text stay with it. No warranty. The scripts in `tools/` are third-party (see
`tools/UPSTREAM.md`) and are not covered by this license.

## For developers

Building from source, project layout and release steps: [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).
Design and roadmap: [`docs/DESIGN.md`](docs/DESIGN.md), [`docs/ROADMAP.md`](docs/ROADMAP.md).
