"""Hard Light wallpaper: the four face-button symbols in their controller diamond, as a hologram projected from
a light pad, on a cold void. 1920x1080 JPEG, kept under the 300 KB themes/README.md asks for.

    PS7_LAYOUT=concept python3 docs/hardlight/themes/hardlight/make_wallpaper.py docs/hardlight/themes/hardlight/wallpaper.jpg
    python3 docs/hardlight/themes/hardlight/make_wallpaper.py out.jpg      # the stock-launcher variant (mockups A/B)
    python3 docs/hardlight/themes/hardlight/make_wallpaper.py out.jpg 2    # 2 = scale, for a 4K preview

PS7_LAYOUT=concept puts a bigger diamond on the right, above the rail layout's tiles; that is the theme's
wallpaper.jpg. Without it the diamond sits top-centre (x 850-1350, y 90-450), the area the stock launcher's two
layouts leave free. Everything under the pad fades to the void so tiles and names stay readable, and grain is
added because the launcher decodes wallpapers in RGB_565 by default, which bands dark gradients.
Needs Pillow, numpy and scipy.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "wallpaper.jpg")
K = float(sys.argv[2]) if len(sys.argv) > 2 else 1.0
W, H = int(1920 * K), int(1080 * K)
SS = 3  # supersampling for the line masks

VOID = (0x04, 0x05, 0x0A)
GRAPHITE = (0x0E, 0x11, 0x20)
MINT = (0x3D, 0xF5, 0xC4)    # triangle
CORAL = (0xFF, 0x4A, 0x5C)   # circle
ICE = (0x6F, 0xA8, 0xFF)     # cross
ORCHID = (0xFF, 0x78, 0xD2)  # square
FROST = (0xE9, 0xED, 0xF7)

# PS7_LAYOUT=concept draws the bigger, right-hand diamond the concept mockup (C) uses; the theme uses the default.
CONCEPT = os.environ.get("PS7_LAYOUT") == "concept"
CX, CY = (1490 * K, 352 * K) if CONCEPT else (1100 * K, 262 * K)  # diamond centre
OFF = (150 if CONCEPT else 118) * K   # centre to each symbol
SIZE = (58 if CONCEPT else 46) * K    # symbol half size
PAD_Y = (612 if CONCEPT else 448) * K
S = OFF / (118 * K)                   # how much bigger than the theme's diamond
rng = np.random.default_rng(7)


def col(c):
    return np.array(c, np.float32) / 255.0


def lin(c):
    return col(c) ** 2.2


# ---- 3D: the diamond is turned a little toward the screen's left, where the clock and hero are ----------------
YAW, PITCH, FOCAL = math.radians(-16), math.radians(7), 1500 * K


def project(x, y, z=0.0):
    """Local diamond coords (px, y down) to screen."""
    x1 = x * math.cos(YAW) + z * math.sin(YAW)
    z1 = -x * math.sin(YAW) + z * math.cos(YAW)
    y1 = y * math.cos(PITCH) - z1 * math.sin(PITCH)
    z2 = y * math.sin(PITCH) + z1 * math.cos(PITCH)
    s = FOCAL / (FOCAL + z2)
    return CX + x1 * s, CY + y1 * s


def shape(kind, ox, oy, r):
    """Closed polylines (lists of local points) for one symbol."""
    if kind == "circle":
        return [[(ox + r * math.cos(t), oy + r * math.sin(t)) for t in np.linspace(0, 2 * math.pi, 97)]]
    if kind == "triangle":
        pts = [(ox + r * 1.08 * math.cos(a), oy + 10 * K + r * 1.08 * math.sin(a))
               for a in (-math.pi / 2, math.pi / 6, 5 * math.pi / 6)]
        return [pts + [pts[0]]]
    if kind == "square":
        q = r * 0.86
        pts = [(ox - q, oy - q), (ox + q, oy - q), (ox + q, oy + q), (ox - q, oy + q)]
        return [pts + [pts[0]]]
    if kind == "cross":
        q = r * 0.86
        return [[(ox - q, oy - q), (ox + q, oy + q)], [(ox + q, oy - q), (ox - q, oy + q)]]
    raise ValueError(kind)


def stroke_mask(lines, width):
    """Anti-aliased mask (float 0..1) of screen-space polylines."""
    m = Image.new("L", (W * SS, H * SS), 0)
    d = ImageDraw.Draw(m)
    for pts in lines:
        p = [(x * SS, y * SS) for x, y in pts]
        d.line(p, fill=255, width=max(1, int(round(width * SS))), joint="curve")
        rr = width * SS / 2
        for x, y in (p[0], p[-1]):
            d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=255)
    m = m.resize((W, H), Image.BOX)
    return np.asarray(m, np.float32) / 255.0


def densify(pts, step=6.0):
    out = []
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        n = max(1, int(math.hypot(x1 - x0, y1 - y0) / (step * K)))
        out += [(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n) for i in range(n)]
    out.append(pts[-1])
    return out


yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
img = np.zeros((H, W, 3), np.float32)

# ---- the void, lifted a little behind the motif, darkest at the bottom ----------------------------------------
img[:] = lin(VOID)
lift = np.exp(-(((xx - CX) / (760 * K)) ** 2 + ((yy - 300 * K) / (430 * K)) ** 2))
img += lift[..., None] * (lin(GRAPHITE) - lin(VOID)) * 1.0
# a cold breath of the four colours, very low, so the lift isn't flat grey
for c, (dx, dy) in ((MINT, (0, -170)), (CORAL, (170, 0)), (ICE, (0, 170)), (ORCHID, (-170, 0))):
    g = np.exp(-(((xx - CX - dx * K) / (330 * K)) ** 2 + ((yy - CY - dy * K) / (260 * K)) ** 2))
    img += g[..., None] * lin(c) * 0.010
floor = np.clip((yy - PAD_Y - 22 * K) / (380 * K), 0, 1) ** 1.2
img *= (1 - 0.55 * floor)[..., None]

# ---- projection cone from the pad up to the diamond ---------------------------------------------------------
t = np.clip((PAD_Y - yy) / (PAD_Y - CY + 190 * K * S), 0, 1)          # 0 at the pad, 1 at the top
half = (250 - 40 * t) * K * S                                    # cone narrows a little going up
edge = np.clip(1 - np.abs(xx - CX) / half, 0, 1)
cone = (edge ** 1.5) * (1 - t) ** 1.7 * (yy < PAD_Y)
img += cone[..., None] * lin(FROST) * 0.022

# ---- the pad: a thin ellipse and the horizon hairline it sits on ---------------------------------------------
pad = [(CX + 232 * K * S * math.cos(a), PAD_Y + 20 * K * S * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 181)]
pad_core = stroke_mask([pad], 1.6 * K)
pad_inner = [(CX + 150 * K * S * math.cos(a), PAD_Y + 12 * K * S * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 181)]
pad_core2 = stroke_mask([pad_inner], 1.0 * K) * 0.45
horizon = np.exp(-((yy - PAD_Y) / (0.7 * K)) ** 2) * np.clip(1 - np.abs(xx - CX) / (560 * K * S), 0, 1) ** 2.2
if CONCEPT:
    horizon *= 0  # the concept's horizon is the tile rail, drawn by the page
pad_light = pad_core + pad_core2 + horizon * 0.8
for sigma, w in ((2, 0.9), (9, 0.5), (34, 0.35)):
    pad_light = pad_light + gaussian_filter(pad_core + horizon, sigma * K) * w
img += pad_light[..., None] * lin(FROST) * 0.42

# ---- the four symbols, each extruded: a bright front face and a dimmer back face 22 px deep -------------------
SYMBOLS = (("triangle", 0, -OFF, MINT, 1.00), ("circle", OFF, 0, CORAL, 1.00),
           ("cross", 0, OFF, ICE, 1.00), ("square", -OFF, 0, ORCHID, 0.92))
light = np.zeros_like(img)
for kind, ox, oy, c, gain in SYMBOLS:
    polys = shape(kind, ox, oy, SIZE)
    front = [[project(x, y, 0) for x, y in densify(p)] for p in polys]
    back = [[project(x, y, 22 * K) for x, y in densify(p)] for p in polys]
    core = stroke_mask(front, 3.2 * K)
    hot = stroke_mask(front, 1.2 * K)               # white-hot centre of the line
    rear = stroke_mask(back, 1.6 * K) * 0.35
    # chromatic fringe: a cooler copy a pixel to the left, a warmer one to the right
    fringe_l = np.roll(core, -int(round(2 * K)), axis=1) * 0.18
    fringe_r = np.roll(core, int(round(2 * K)), axis=1) * 0.18
    base = core + rear
    bloom = sum(gaussian_filter(base, s * K) * w for s, w in ((3, 1.1), (12, 0.8), (40, 0.4)))
    lc = lin(c) * gain
    light += (base + bloom)[..., None] * lc * 1.25
    light += hot[..., None] * (lin(FROST) * 0.55 + lc * 0.45)
    light += fringe_l[..., None] * lin(ICE) + fringe_r[..., None] * lin(CORAL)

# hologram scan: faint horizontal lines through the symbols only, every 3 px
scan = 0.86 + 0.14 * (np.sin(yy * (2 * math.pi / (3 * K))) * 0.5 + 0.5)
img += light * scan[..., None]

# the floor under the pad mirrors the symbols, soft and faint, fading over 170 px
ref = np.zeros_like(light)
n = int(170 * K)
p0 = int(PAD_Y)
src = light[p0 - n:p0][::-1]                      # rows just above the pad, flipped
fade = (1 - np.linspace(0, 1, n, dtype=np.float32)) ** 2
ref[p0:p0 + n] = src * fade[:, None, None]
ref = gaussian_filter(ref, (5 * K, 2 * K, 0))
img += ref * 0.10

# ---- motes drifting in the cone ------------------------------------------------------------------------------
motes = np.zeros((H, W), np.float32)
for _ in range(70):
    y = PAD_Y - rng.uniform(8, 338) * K * S
    tt = (PAD_Y - y) / (PAD_Y - CY + 190 * K * S)
    x = CX + rng.normal(0, 0.45) * (250 - 40 * tt) * K * S
    if 0 <= x < W and 0 <= y < H:
        motes[int(y), int(x)] += rng.uniform(0.3, 1.0) * (1 - tt * 0.6)
motes = gaussian_filter(motes, 0.9 * K) * 6 + gaussian_filter(motes, 4 * K) * 3
img += motes[..., None] * lin(FROST) * 0.25

# ---- tone map, back to sRGB, grain, save --------------------------------------------------------------------
img = 1 - np.exp(-img * 1.15)                        # soft shoulder: hot cores roll off instead of clipping
srgb = np.clip(img, 0, 1) ** (1 / 2.2) * 255
srgb += rng.normal(0, 1.1, srgb.shape[:2])[..., None]  # luminance grain, breaks RGB_565 bands
out = Image.fromarray(np.clip(srgb + 0.5, 0, 255).astype(np.uint8), "RGB")
out.save(OUT, "JPEG", quality=88, optimize=True, progressive=True, subsampling=0)
print(OUT, out.size, os.path.getsize(OUT), "bytes")
