#!/usr/bin/env python3
"""Draws the maze decals (64x64 RGBA) and writes their block models and blockstates.

Run: python3 tools/gen_decals.py   (needs Pillow and numpy)
Floor decals (block vet_san) and wall decals (block vet_tuong) each have 9 kinds; the order must match vn.cavemind.gear.Vet.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "cavemind")
TEX = os.path.join(ROOT, "textures", "block", "vet")
N = 64
FLOOR = ["arc", "chalk", "board", "mud", "hands", "wax", "wet", "drag", "puddle"]
WALL = ["stain_top", "stain_mid", "stain_bot", "hand", "claw", "mold", "crack", "peel", "boards"]


def canvas():
    return Image.new("RGBA", (N, N), (0, 0, 0, 0))


def soft(img, r):
    return img.filter(ImageFilter.GaussianBlur(r))


def blotch(rng, cx, cy, r, n=14):
    pts = []
    for i in range(n):
        a = i / n * math.tau
        rr = r * rng.uniform(0.65, 1.25)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return pts


def mask_noise(rng, scale=8, amt=0.35):
    small = rng.random((N // scale + 2, N // scale + 2))
    big = np.array(Image.fromarray((small * 255).astype(np.uint8)).resize((N, N), Image.BICUBIC)) / 255.0
    return 1 - amt + amt * big


def apply_alpha_noise(img, rng, scale=8, amt=0.35):
    a = np.array(img).astype(float)
    a[..., 3] *= mask_noise(rng, scale, amt)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


# ---------- floor ----------

def arc(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    hx, hy = 6, -2  # hinge near the top-left: the door swings out over this tile
    for k in range(7):
        r = 26 + k * 4.2 + rng.uniform(-1, 1)
        start, end = rng.uniform(8, 20), rng.uniform(70, 88)
        box = [hx - r, hy - r, hx + r, hy + r]
        d.arc(box, start, end, fill=(40, 26, 16, 210), width=2 if k % 3 else 1)
        d.arc([b + (0.8 if i < 2 else -0.8) for i, b in enumerate(box)], start, end, fill=(170, 140, 100, 90), width=1)
    return apply_alpha_noise(img, rng, 4, 0.5)


def chalk(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    for k in range(3):
        r = 25 + rng.uniform(-1.5, 1.5)
        pts = []
        for i in range(73):
            a = i / 72 * math.tau + k * 0.07
            rr = r + rng.uniform(-1.2, 1.2)
            pts.append((32 + math.cos(a) * rr, 32 + math.sin(a) * rr))
        d.line(pts, fill=(235, 232, 220, 150 + k * 25), width=2)
    for _ in range(40):
        x, y = rng.uniform(4, 60), rng.uniform(4, 60)
        if abs(math.hypot(x - 32, y - 32) - 25) < 4:
            d.point((x, y), fill=(240, 238, 228, 120))
    return apply_alpha_noise(soft(img, 0.4), rng, 4, 0.55)


def board(rng):
    # "ô ăn quan": two rows of five squares with a half circle at each end, chalked on the floor
    img = canvas()
    d = ImageDraw.Draw(img)
    c = (232, 228, 214, 200)
    x0, x1, y0, y1 = 12, 52, 22, 42
    d.rectangle([x0, y0, x1, y1], outline=c, width=1)
    d.line([x0, 32, x1, 32], fill=c, width=1)
    for i in range(1, 5):
        x = x0 + i * 8
        d.line([x, y0, x, y1], fill=c, width=1)
    d.arc([x0 - 10, y0, x0 + 10, y1], 90, 270, fill=c, width=1)
    d.arc([x1 - 10, y0, x1 + 10, y1], 270, 90, fill=c, width=1)
    for _ in range(14):
        x, y = rng.uniform(x0 + 2, x1 - 2), rng.uniform(y0 + 2, y1 - 2)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(120, 110, 100, 220))
    return apply_alpha_noise(img, rng, 4, 0.45)


def mud(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon(blotch(rng, 30, 34, 17, 18), fill=(28, 22, 16, 235))
    for _ in range(14):
        a, r = rng.uniform(0, math.tau), rng.uniform(17, 28)
        x, y, s = 30 + math.cos(a) * r, 34 + math.sin(a) * r, rng.uniform(1.2, 3.5)
        d.ellipse([x - s, y - s, x + s, y + s], fill=(32, 25, 18, 220))
    return apply_alpha_noise(soft(img, 0.8), rng, 6, 0.3)


def hand(d, x, y, s, ang, col):
    ca, sa = math.cos(ang), math.sin(ang)

    def p(u, v):
        return (x + (u * ca - v * sa) * s, y + (u * sa + v * ca) * s)

    d.polygon([p(-4, 0), p(4, 0), p(4.5, 6), p(-4.5, 6)], fill=col)
    d.ellipse([p(-4.5, 2)[0] - 4 * s, p(-4.5, 2)[1] - 4 * s, p(-4.5, 2)[0] + 4 * s, p(-4.5, 2)[1] + 4 * s], fill=col)
    for u, l in ((-3.2, 6), (-1.1, 7.2), (1.1, 7.6), (3.2, 6.4)):
        a, b = p(u, 0), p(u, -l)
        d.line([a, b], fill=col, width=max(1, int(2.2 * s)))
    a, b = p(4.2, 4), p(8, 1)
    d.line([a, b], fill=col, width=max(1, int(2.2 * s)))


def hands(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    col = (34, 26, 18, 215)
    hand(d, 22, 30, 1.6, -0.4, col)
    hand(d, 42, 40, 1.5, 0.35, col)
    d.line([(14, 48), (8, 62)], fill=(34, 26, 18, 160), width=3)
    return apply_alpha_noise(soft(img, 0.5), rng, 4, 0.4)


def wax(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    for _ in range(9):
        x, y, r = rng.uniform(10, 54), rng.uniform(10, 54), rng.uniform(5, 13)
        tone = rng.randint(205, 240)
        d.polygon(blotch(rng, x, y, r, 12), fill=(tone, tone - 8, tone - 30, 235))
        d.polygon(blotch(rng, x, y, r * 0.55, 10), fill=(tone + 10 if tone < 245 else 250, tone, tone - 22, 240))
    for _ in range(6):
        x, y = rng.uniform(14, 50), rng.uniform(14, 50)
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(60, 50, 40, 200))  # burnt wick stubs
    return soft(img, 0.5)


def wet(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon(blotch(rng, 32, 32, 24, 20), fill=(14, 18, 22, 150))
    img = soft(img, 3)
    d = ImageDraw.Draw(img)
    for _ in range(5):
        x, y = rng.uniform(16, 48), rng.uniform(16, 48)
        d.ellipse([x - 3, y - 1, x + 3, y + 1], fill=(160, 190, 200, 60))
    return img


def drag(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    for k in range(4):
        x = 20 + k * 7 + rng.uniform(-1, 1)
        pts = [(x + math.sin(y / 9 + k) * 2, y) for y in range(0, 65, 4)]
        d.line(pts, fill=(70, 22, 18, 170), width=2 if k % 2 else 3)
    return apply_alpha_noise(soft(img, 0.6), rng, 4, 0.6)


def puddle(rng):
    # a shallow film of water: dark, with streaks that catch the camera's light
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon(blotch(rng, 32, 32, 25, 22), fill=(18, 26, 32, 175))
    img = soft(img, 1.6)
    d = ImageDraw.Draw(img)
    for _ in range(7):
        x, y, l = rng.uniform(14, 46), rng.uniform(14, 46), rng.uniform(4, 11)
        d.line([(x, y), (x + l, y - l * 0.35)], fill=(190, 215, 225, 110), width=1)
    ring = canvas()
    ImageDraw.Draw(ring).polygon(blotch(rng, 32, 32, 24, 22), outline=(120, 140, 150, 90))
    return Image.alpha_composite(img, soft(ring, 0.6))


# ---------- wall ----------

def streaks(rng, top, bottom, seed_cols=6):
    img = canvas()
    d = ImageDraw.Draw(img)
    for _ in range(seed_cols):
        x, w = rng.uniform(8, 56), rng.uniform(2, 7)
        y0 = 0 if top != "start" else rng.uniform(0, 10)
        y1 = N if bottom != "end" else rng.uniform(36, 60)
        col = (22, 20, 16, rng.randint(120, 190))
        for y in range(int(y0), int(y1)):
            wx = w * (1 - 0.5 * (y - y0) / max(1, y1 - y0)) if bottom == "end" else w
            xx = x + math.sin(y / 11 + x) * 1.2
            d.line([(xx - wx / 2, y), (xx + wx / 2, y)], fill=col)
        if bottom == "end":
            d.ellipse([x - w * 0.6, y1 - 1, x + w * 0.6, y1 + w], fill=col)
    if top == "start":
        band = canvas()
        bd = ImageDraw.Draw(band)
        bd.rectangle([0, 0, N, 9], fill=(20, 18, 14, 170))
        img = Image.alpha_composite(soft(band, 2.5), img)
    if bottom == "end":
        tide = canvas()
        td = ImageDraw.Draw(tide)
        td.rectangle([0, 56, N, N], fill=(26, 22, 16, 120))
        img = Image.alpha_composite(img, soft(tide, 2))
    return apply_alpha_noise(soft(img, 0.8), rng, 6, 0.35)


def stain_top(rng):
    return streaks(rng, "start", "through")


def stain_mid(rng):
    return streaks(rng, "through", "through", 5)


def stain_bot(rng):
    return streaks(rng, "through", "end", 5)


def wall_hand(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    col = (60, 14, 12, 210)
    hand(d, 30, 30, 2.1, 0.12, col)
    for k in range(4):
        x = 22 + k * 5
        d.line([(x, 40), (x + rng.uniform(-2, 2), 40 + rng.uniform(10, 22))], fill=(60, 14, 12, 150), width=2)
    return apply_alpha_noise(soft(img, 0.5), rng, 4, 0.35)


def claw(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    for g in range(2):
        ox, oy = 10 + g * 22, 8 + g * 14
        for k in range(4):
            a = (ox + k * 5, oy)
            b = (ox + k * 5 + 12, oy + 36)
            d.line([a, b], fill=(20, 14, 10, 220), width=2)
            d.line([(a[0] + 1.5, a[1]), (b[0] + 1.5, b[1])], fill=(200, 180, 150, 110), width=1)
    return apply_alpha_noise(img, rng, 4, 0.45)


def mold(rng):
    img = canvas()
    d = ImageDraw.Draw(img)
    for _ in range(26):
        x, y = rng.uniform(0, N), rng.uniform(30, N) if rng.random() < 0.8 else rng.uniform(0, 30)
        r = rng.uniform(1.5, 6)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(14, 18, 12, rng.randint(140, 220)))
    return apply_alpha_noise(soft(img, 1.0), rng, 4, 0.4)


def crack(rng):
    img = canvas()
    d = ImageDraw.Draw(img)

    def branch(x, y, a, length, depth):
        if depth == 0 or length < 3:
            return
        nx, ny = x + math.cos(a) * length, y + math.sin(a) * length
        d.line([(x, y), (nx, ny)], fill=(30, 26, 20, 200), width=1)
        branch(nx, ny, a + rng.uniform(-0.6, 0.6), length * 0.75, depth - 1)
        if rng.random() < 0.55:
            branch(nx, ny, a + rng.choice([-1, 1]) * rng.uniform(0.6, 1.2), length * 0.55, depth - 1)

    branch(rng.uniform(20, 44), 0, math.pi / 2 + rng.uniform(-0.3, 0.3), 12, 7)
    branch(0, rng.uniform(30, 50), rng.uniform(-0.3, 0.3), 10, 6)
    return img


def peel(rng):
    # plaster flaked away: a ragged hole showing the rough brick underneath
    img = canvas()
    shape = canvas()
    ImageDraw.Draw(shape).polygon(blotch(rng, 32, 32, 22, 22), fill=(255, 255, 255, 255))
    bricks = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bricks)
    for row in range(0, N, 8):
        off = 0 if (row // 8) % 2 == 0 else 8
        for col in range(-off, N, 16):
            tone = rng.randint(120, 160)
            bd.rectangle([col + 1, row + 1, col + 15, row + 7], fill=(tone, int(tone * 0.52), int(tone * 0.38), 255))
        bd.line([(0, row), (N, row)], fill=(95, 88, 78, 255))
    mask = shape.split()[3]
    img.paste(bricks, (0, 0), mask)
    rim = soft(shape, 1.6)
    rim_a = np.array(rim)[..., 3].astype(float)
    hole = np.array(shape)[..., 3].astype(float)
    edge = np.clip(rim_a - hole * 0.85, 0, 255)
    out = np.array(img).astype(float)
    shadow = np.zeros_like(out)
    shadow[..., 3] = edge * 0.9
    shadow[..., 0:3] = 30
    base = Image.fromarray(np.clip(shadow, 0, 255).astype(np.uint8))
    return Image.alpha_composite(base, Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)))


def boards(rng):
    # rotten planks nailed across a doorway in an X, with one plank straight across
    img = canvas()
    d = ImageDraw.Draw(img)

    def plank(a, b, w, tone):
        d.line([a, b], fill=(24, 18, 12, 255), width=w + 2)
        d.line([a, b], fill=(tone, int(tone * 0.74), int(tone * 0.52), 250), width=w)
        for k in range(-w // 2 + 2, w // 2 - 1, 3):
            dx, dy = b[0] - a[0], b[1] - a[1]
            ln = math.hypot(dx, dy)
            nx, ny = -dy / ln * k, dx / ln * k
            d.line([(a[0] + nx, a[1] + ny), (b[0] + nx, b[1] + ny)], fill=(int(tone * 0.7), int(tone * 0.5), int(tone * 0.34), 160), width=1)
        for p in (a, b):
            q = (p[0] + (b[0] - a[0]) * (0.08 if p == a else -0.08), p[1] + (b[1] - a[1]) * (0.08 if p == a else -0.08))
            d.ellipse([q[0] - 1.5, q[1] - 1.5, q[0] + 1.5, q[1] + 1.5], fill=(60, 58, 55, 255))

    plank((2, 6), (62, 58), 11, rng.randint(125, 145))
    plank((2, 58), (62, 6), 11, rng.randint(110, 130))
    plank((0, 30), (64, 34), 9, rng.randint(135, 155))
    return apply_alpha_noise(img, rng, 4, 0.12)


FLOOR_FN = [arc, chalk, board, mud, hands, wax, wet, drag, puddle]
WALL_FN = [stain_top, stain_mid, stain_bot, wall_hand, claw, mold, crack, peel, boards]

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def model_files(block, names, template):
    models = os.path.join(ROOT, "models", "block")
    with open(os.path.join(models, block + ".json"), "w") as f:
        json.dump(template, f, indent=1)
    for k, name in enumerate(names):
        prefix = "san" if block == "vet_san" else "tuong"
        with open(os.path.join(models, f"{block}_{k}.json"), "w") as f:
            json.dump({"parent": f"cavemind:block/{block}", "textures": {"vet": f"cavemind:block/vet/{prefix}_{name}"}}, f)
    variants = {}
    for k in range(len(names)):
        for facing, y in FACING_Y.items():
            v = {"model": f"cavemind:block/{block}_{k}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},kind={k}"] = v
    with open(os.path.join(ROOT, "blockstates", block + ".json"), "w") as f:
        json.dump({"variants": variants}, f, indent=1)


def main():
    os.makedirs(TEX, exist_ok=True)
    for k, fn in enumerate(FLOOR_FN):
        img = fn_call(fn, 100 + k)
        img.save(os.path.join(TEX, "san_" + FLOOR[k] + ".png"))
    for k, fn in enumerate(WALL_FN):
        img = fn_call(fn, 200 + k)
        img.save(os.path.join(TEX, "tuong_" + WALL[k] + ".png"))
    floor = {
        "ambientocclusion": False,
        "render_type": "minecraft:translucent",
        "textures": {"particle": "#vet"},
        "elements": [{"from": [0, 0.04, 0], "to": [16, 0.08, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#vet"}}}],
    }
    wall = {
        "ambientocclusion": False,
        "render_type": "minecraft:translucent",
        "textures": {"particle": "#vet"},
        "elements": [{"from": [0, 0, 15.8], "to": [16, 16, 15.9], "shade": False,
                      "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#vet"}}}],
    }
    model_files("vet_san", FLOOR, floor)
    model_files("vet_tuong", WALL, wall)
    print("decals written")


class _Rng:
    """random.Random for Python-side choices plus numpy draws for noise masks."""

    def __init__(self, seed):
        self.r = random.Random(seed)
        self.np = np.random.default_rng(seed)

    def uniform(self, a, b):
        return self.r.uniform(a, b)

    def randint(self, a, b):
        return self.r.randint(a, b)

    def choice(self, xs):
        return self.r.choice(xs)

    def random(self, shape=None):
        return self.r.random() if shape is None else self.np.random(shape)


def fn_call(fn, seed):
    return fn(_Rng(seed))


if __name__ == "__main__":
    main()
