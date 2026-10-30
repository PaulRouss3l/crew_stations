#!/usr/bin/env python3
"""Forum signature banners: one small clickable banner per mod, cut from each mod's forum banner.

Usage: python3 art/signature.py [--sts /path/to/slay-the-sector]
Writes docs/assets/signature/crew-stations.png and slay-the-sector.png (150x100: the forum allows signature
images up to 150x256). The BBCode that uses them is
docs/FORUM_SIGNATURE.bbcode. Orbitron (SIL OFL) is read from where TriOS installs it, as in banner.py.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
FONTS = Path.home() / "Downloads/TriOS/data/flutter_assets/assets/fonts"
OUT = HERE.parent / "docs" / "assets" / "signature"
SIZE = (150, 100)


def make(banner, crop, title, subtitle, out):
    """{@code crop}: (left, top, width) of the banner area to use; its height follows SIZE's shape."""
    src = Image.open(banner).convert("RGB")
    left, top, cw = crop
    ch = round(cw * SIZE[1] / SIZE[0])
    img = src.crop((left, top, left + cw, top + ch)).resize(SIZE, Image.LANCZOS)

    # Darken the bottom half so the text reads on any art.
    shade = Image.new("L", SIZE, 0)
    d = ImageDraw.Draw(shade)
    for y in range(SIZE[1] // 3, SIZE[1]):
        d.line([(0, y), (SIZE[0], y)], fill=int(200 * (y - SIZE[1] / 3) / (SIZE[1] * 2 / 3)))
    img = Image.composite(Image.new("RGB", SIZE, (0, 0, 0)), img, shade)

    d = ImageDraw.Draw(img)
    tf = ImageFont.truetype(str(FONTS / "Orbitron-Bold.ttf"), 13)
    sf = ImageFont.truetype(str(FONTS / "Orbitron-Medium.ttf"), 8)
    tw = d.textbbox((0, 0), title, font=tf)[2]
    sw = d.textbbox((0, 0), subtitle, font=sf)[2]
    d.text(((SIZE[0] - tw) // 2, 66), title, font=tf, fill=(232, 246, 255))
    d.text(((SIZE[0] - sw) // 2, 85), subtitle, font=sf, fill=(95, 205, 255))
    d.rectangle([0, 0, SIZE[0] - 1, SIZE[1] - 1], outline=(43, 101, 117), width=2)
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, optimize=True)
    print(out.relative_to(HERE.parent))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sts", default=str(HERE.parent.parent / "slay-the-sector"))
    a = ap.parse_args()
    make(HERE.parent / "docs/assets/banner.png", (470, 0, 660), "CREW STATIONS", "STATION YARD INC.",
         OUT / "crew-stations.png")
    make(Path(a.sts) / "docs/assets/banner.png", (500, 0, 620), "SLAY THE SECTOR", "ROGUELITE CAMPAIGN",
         OUT / "slay-the-sector.png")


if __name__ == "__main__":
    main()
