#!/usr/bin/env python3
"""Forum banner: art/raw/banner_<seed>.png + title and subtitle -> docs/assets/banner.png (1600x640).

Usage: python3 art/banner.py <seed> [--font Orbitron-Bold.ttf] [--font-sub Orbitron-Medium.ttf]
Orbitron (SIL Open Font License) is not shipped with the repo: pass its path, or keep it where TriOS installs it.
Same layout as Slay the Sector's banner: glowing title near the bottom, cyan subtitle, thin teal frame.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).resolve().parent
FONTS = Path.home() / "Downloads/TriOS/data/flutter_assets/assets/fonts"
OUT = HERE.parent / "docs" / "assets" / "banner.png"
TITLE = "CREW STATIONS"
SUBTITLE = "BUILD AND UPGRADE CREWED STATIONS ABOARD YOUR SHIPS"
SIZE = (1600, 640)


def centered(draw, y, text, font):
    w = draw.textbbox((0, 0), text, font=font)[2]
    return ((SIZE[0] - w) // 2, y)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("seed")
    ap.add_argument("--font", default=str(FONTS / "Orbitron-Bold.ttf"))
    ap.add_argument("--font-sub", default=str(FONTS / "Orbitron-Medium.ttf"))
    a = ap.parse_args()

    img = Image.open(HERE / "raw" / f"banner_{a.seed}.png").convert("RGB").resize(SIZE, Image.LANCZOS)
    title_font = ImageFont.truetype(a.font, 112)
    sub_font = ImageFont.truetype(a.font_sub, 30)

    # Darken the bottom so the text reads on any art.
    shade = Image.new("L", SIZE, 0)
    sd = ImageDraw.Draw(shade)
    for y in range(SIZE[1] // 2, SIZE[1]):
        sd.line([(0, y), (SIZE[0], y)], fill=int(170 * (y - SIZE[1] / 2) / (SIZE[1] / 2)))
    img = Image.composite(Image.new("RGB", SIZE, (0, 0, 0)), img, shade)

    title_pos = centered(ImageDraw.Draw(img), 448, TITLE, title_font)
    sub_pos = centered(ImageDraw.Draw(img), 575, SUBTITLE, sub_font)

    glow = Image.new("RGB", SIZE, (0, 0, 0))
    ImageDraw.Draw(glow).text(title_pos, TITLE, font=title_font, fill=(60, 190, 255))
    glow = glow.filter(ImageFilter.GaussianBlur(14))
    img = Image.blend(img, Image.composite(glow, img, glow.convert("L")), 0.9)

    d = ImageDraw.Draw(img)
    d.text(title_pos, TITLE, font=title_font, fill=(232, 246, 255))
    d.text(sub_pos, SUBTITLE, font=sub_font, fill=(95, 205, 255))
    d.rectangle([0, 0, SIZE[0] - 1, SIZE[1] - 1], outline=(43, 101, 117), width=4)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, optimize=True)
    print(OUT.relative_to(HERE.parent))


if __name__ == "__main__":
    main()
