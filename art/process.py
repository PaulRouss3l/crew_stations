#!/usr/bin/env python3
"""Preview and install Station illustrations. See docs/ART.md.

  python3 art/process.py preview [out.png]   contact sheet of art/raw/*.png with the in-game crops at 2x
  python3 art/process.py install <station> <seed>
                                            art/raw/<station>_<seed>.png -> graphics/crew_stations/stations/<station>.png (512x512)
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
RAW = HERE / "raw"
OUT = HERE.parent / "graphics" / "crew_stations" / "stations"
SIZE = 512
# On-screen boxes the screen crops into (ShipyardScreen): card banner (3 cards) and picker tile.
BANNER = (263, 119)
TILE = (118, 181)


def cover(img, box):
    """Centre crop of img to box's aspect ratio, resized to box (what Painter draws in cover mode)."""
    w, h = img.size
    ar = box[0] / box[1]
    cw, ch = (w, round(w / ar)) if w / h < ar else (round(h * ar), h)
    left, top = (w - cw) // 2, (h - ch) // 2
    return img.crop((left, top, left + cw, top + ch)).resize(box, Image.LANCZOS)


def preview(out):
    files = sorted(f for f in RAW.glob("*_*.png"))
    if not files:
        sys.exit("no images in art/raw")
    s = 2
    bw, bh, tw, th, sq = BANNER[0] * s, BANNER[1] * s, TILE[0] * s, TILE[1] * s, 240
    row_h, pad = max(bh, th, sq) + 30, 16
    sheet = Image.new("RGB", (sq + bw + tw + 4 * pad, len(files) * row_h + pad), (5, 9, 12))
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        img = Image.open(f).convert("RGB")
        y = pad + i * row_h
        d.text((pad, y), f.stem, fill=(214, 241, 255))
        sheet.paste(img.resize((sq, sq), Image.LANCZOS), (pad, y + 16))
        sheet.paste(cover(img, BANNER).resize((bw, bh), Image.LANCZOS), (2 * pad + sq, y + 16))
        sheet.paste(cover(img, TILE).resize((tw, th), Image.LANCZOS), (3 * pad + sq + bw, y + 16))
    sheet.save(out)
    print(out)


def install(station, seed):
    src = RAW / f"{station}_{seed}.png"
    OUT.mkdir(parents=True, exist_ok=True)
    dst = OUT / f"{station}.png"
    Image.open(src).convert("RGB").resize((SIZE, SIZE), Image.LANCZOS).save(dst, optimize=True)
    print(f"{src.name} -> {dst.relative_to(HERE.parent)}")


if __name__ == "__main__":
    if len(sys.argv) >= 2 and sys.argv[1] == "preview":
        preview(sys.argv[2] if len(sys.argv) > 2 else str(HERE / "raw" / "preview.png"))
    elif len(sys.argv) == 4 and sys.argv[1] == "install":
        install(sys.argv[2], sys.argv[3])
    else:
        sys.exit(__doc__)
