"""Ocean wallpaper: a photo cropped to 16:9 and graded so the rail layout reads over it. The photo is not drawn
here; pass your own (the theme was made from a breaking wave under a rainbow, beside a green sea cliff).

    python3 docs/hardlight/themes/ocean/make_wallpaper.py <photo.jpg> docs/hardlight/themes/ocean/wallpaper.jpg
    python3 docs/hardlight/themes/ocean/make_wallpaper.py <photo.jpg> out.jpg 0.05   # crop from 5% down instead

The grade is where the layout's text and tiles sit (see GUIDE.md, section 0):
- the deep-sea colour washes in from the left, under the hero name and hints (to about 55% of the width);
- it rises from the bottom, under the rail and its reflections (from about 58% of the height);
- a light veil at the top, under the top rail.
The upper right (sky, rainbow, cliff) is left almost untouched. Grain breaks the bands RGB_565 would show, and
the JPEG quality is lowered until the file is under 300 KB, as themes/README.md asks. Needs Pillow and numpy.
"""
import io
import os
import sys

import numpy as np
from PIL import Image

SRC = sys.argv[1]
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "wallpaper.jpg")
TOP = float(sys.argv[3]) if len(sys.argv) > 3 else 0.03   # where the 16:9 crop starts, as a share of the free height
W, H = 1920, 1080
ABYSS = np.array((0x03, 0x16, 0x1D), np.float32) / 255   # the theme's background_color
LIMIT = 300 * 1024

im = Image.open(SRC).convert("RGB")
w, h = im.size
cw, ch = (w, round(w * 9 / 16)) if w * 9 / 16 <= h else (round(h * 16 / 9), h)
x0 = (w - cw) // 2
y0 = round((h - ch) * TOP)
im = im.crop((x0, y0, x0 + cw, y0 + ch)).resize((W, H), Image.LANCZOS)

a = np.asarray(im, np.float32) / 255   # sRGB: the washes then darken as much as they look like they should
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
u, v = xx / W, yy / H


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


left = 0.80 * smooth((0.55 - u) / 0.55) ** 1.2
bottom = 0.92 * smooth((v - 0.58) / 0.42)
top = 0.45 * smooth((0.16 - v) / 0.16)
veil = 1 - (1 - left) * (1 - bottom) * (1 - top)          # the three washes combined, never over 1
a *= 0.9                                                   # the photo is bright: take it down a little everywhere
a = a * (1 - veil[..., None]) + ABYSS * veil[..., None]

srgb = np.clip(a, 0, 1) * 255
srgb += np.random.default_rng(11).normal(0, 1.0, (H, W))[..., None]
out = Image.fromarray(np.clip(srgb + 0.5, 0, 255).astype(np.uint8), "RGB")

for q in range(88, 59, -2):
    buf = io.BytesIO()
    out.save(buf, "JPEG", quality=q, optimize=True, progressive=True)
    if buf.tell() <= LIMIT:
        break
with open(OUT, "wb") as f:
    f.write(buf.getvalue())
print(OUT, out.size, f"quality {q}", buf.tell(), "bytes")
