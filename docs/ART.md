# ART — Station illustrations

One square illustration per Station type, used in three places by the Shipyard screen. Each place crops
the centre of the image to its own shape (`Painter.sprite(..., cover = true)`):

| Where | Box on screen | Shape | Part of the image shown |
|---|---|---|---|
| Station card banner | ~263×119 (298×119 with 1–2 cards, 193×89 with 4) | 2.2–2.5:1 | centre band, full width |
| Picker tile | ~118×181 | 0.65:1 | centre column, full height |
| Picker detail panel | 62×62 | square | everything |

The **safe area** seen in every place is the centre ~65% of the width × ~45% of the height: keep the
subject there.

## Files

- Shipped: `graphics/crew_stations/stations/<station id>.png`, 512×512 RGB, referenced from
  `data/config/settings.json` (`graphics.crst_stations`).
- Prompts: `art/prompts.json` (shared style + one subject line and accent colour per Station).
- Raw generations: `art/raw/` (git-ignored).

## Pipeline

1. Start ComfyUI (`/home/ubik/localhost/perso/ComfyUI`, `venv/bin/python main.py --listen 127.0.0.1`).
   Model: Z-Image Turbo (nvfp4), Qwen3 4B text encoder, 8 steps, cfg 1, res_multistep, shift 3.
2. `python3 art/generate.py [station ...] --seeds 2 --seed 100` → `art/raw/<station>_<seed>.png`, 1024×1024,
   about 10 s each on the RTX 5060.
3. `python3 art/process.py preview` → `art/raw/preview.png`: each image with its card banner and tile crops
   at 2× in-game size.
4. `python3 art/process.py install <station> <seed>` → resized 512×512 into `graphics/`.
5. Hullmod icon: `python3 art/generate.py hullmod --seeds 4`, then `python3 art/process.py icon <seed>` →
   `graphics/crew_stations/hullmods/crew_stations.png` (32×32, cropped, sharpened, brightened).

## Style

Painterly concept art of a cramped crewed compartment on a worn industrial starship: muted steel grey and dark
teal, two or three crew in work jumpsuits, one accent colour per Station (shield cyan, engine orange, energy
violet, missile red...). No text, logos or UI. Full prompts: `art/prompts.json`.
