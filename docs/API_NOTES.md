# API_NOTES — Crew Stations

Verified Starsector 0.98a API facts and pitfalls relevant to this mod. Verify with
`~/.claude/skills/starsector-modding/scripts/api_lookup.py` before adding an entry.
See also the sister project's notes: `/home/ubik/localhost/perso/starsector/slay-the-sector/docs/API_NOTES.md`.

## UI input

- **A consumed `InputEventAPI` throws on any data access** (`containsEvent`, `isMouseMoveEvent`, ...):
  `RuntimeException: Attempt to access data of a consumed InputEvent`. Check `isConsumed()` first, every
  time. Crashed the game when ESC (consumed by the screen) reached `Painter.handle` (fixed 2026-10-30).
- **`TooltipMakerAPI.addPara(String, Color, float)` does not run `String.format`**; the highlight overloads
  (`Color hl`, `Color[]`) do. User text (ship names) only through the plain overload or as format arguments.
- **`addCustom` places each component below-left of the previous one**, inheriting its x: an
  `setXAlignOffset` on every row accumulates. Offset the first only.
