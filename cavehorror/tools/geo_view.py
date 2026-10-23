import json, math, sys, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def num(v):
    try:
        return float(v)
    except Exception:
        return 0.0


def vec(v):
    if isinstance(v, dict):
        v = v.get('vector', v.get('post', v.get('pre', [0, 0, 0])))
        if isinstance(v, dict):
            v = v.get('vector', [0, 0, 0])
    if isinstance(v, (int, float, str)):
        v = [v, v, v]
    return [num(x) for x in v]


def sample(ch, t):
    if isinstance(ch, list) or (isinstance(ch, dict) and 'vector' in ch):
        return vec(ch)
    keys = sorted((float(k), v) for k, v in ch.items())
    if t <= keys[0][0]:
        return vec(keys[0][1])
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            u = 0 if t1 == t0 else (t - t0) / (t1 - t0)
            va, vb = vec(a), vec(b)
            return [x + (y - x) * u for x, y in zip(va, vb)]
    return vec(keys[-1][1])


def load(geo_path, anim_path, anim, t):
    geo = json.load(open(geo_path, encoding='utf-8'))['minecraft:geometry'][0]
    bones = {b['name']: b for b in geo['bones']}
    A = json.load(open(anim_path, encoding='utf-8'))['animations'].get(anim, {}) if anim else {}
    if anim and A.get('animation_length') and t is None:
        t = float(A['animation_length'])
    t = t or 0.0
    ab = A.get('bones', {})
    pose = {}
    for name in bones:
        ch = ab.get(name, {})
        pose[name] = {k: sample(ch[k], t) if k in ch else ([1, 1, 1] if k == 'scale' else [0, 0, 0]) for k in ('rotation', 'position', 'scale')}
    return bones, pose


def quads(bones, pose, yaw=0.0, mats=None):
    D = math.radians
    world = ry(D(180 - yaw)) @ np.diag([1 / 16, 1 / 16, 1 / 16, 1])
    out = []
    kids = {}
    for b in bones.values():
        kids.setdefault(b.get('parent'), []).append(b['name'])

    def rec(name, M):
        b = bones[name]
        p = b.get('pivot', [0, 0, 0])
        px, py, pz = -p[0], p[1], p[2]
        r0 = b.get('rotation', [0, 0, 0])
        ar = pose[name]['rotation']
        ap = pose[name]['position']
        rX = D(-r0[0]) + D(-ar[0])
        rY = D(-r0[1]) + D(-ar[1])
        rZ = D(r0[2]) + D(ar[2])
        M = M @ tr(-ap[0], ap[1], ap[2]) @ tr(px, py, pz) @ rz(rZ) @ ry(rY) @ rx(rX) @ tr(-px, -py, -pz)
        if mats is not None:
            mats[name] = world @ M
        for c in b.get('cubes', []):
            o, s = c['origin'], c['size']
            ox, oy, oz = -(o[0] + s[0]), o[1], o[2]
            cp = c.get('pivot', [0, 0, 0])
            cr = c.get('rotation', [0, 0, 0])
            C = M @ tr(-cp[0], cp[1], cp[2]) @ rz(D(cr[2])) @ ry(D(-cr[1])) @ rx(D(-cr[0])) @ tr(cp[0], -cp[1], -cp[2])
            xs, ys, zs = (ox, ox + s[0]), (oy, oy + s[1]), (oz, oz + s[2])
            v = {}
            for i, x in enumerate(xs):
                for j, y in enumerate(ys):
                    for k, z in enumerate(zs):
                        v[(i, j, k)] = (world @ C @ np.array([x, y, z, 1.0]))[:3]
            faces = [[(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)],
                     [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)], [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)],
                     [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]]
            for f in faces:
                out.append((name, [v[k] for k in f]))
        for k in kids.get(name, []):
            rec(k, M)

    for root in kids.get(None, []):
        rec(root, np.eye(4))
    return out


VIEWS = {
    'front': (lambda p: (p[0], p[1]), lambda p: p[2]),
    'side': (lambda p: (-p[2], p[1]), lambda p: p[0]),
    'top': (lambda p: (p[0], -p[2]), lambda p: p[1]),
}
HL = {'righthand': (220, 90, 60), 'finger5': (220, 90, 60), 'finger6': (230, 140, 60), 'finger7': (220, 90, 60), 'finger8': (220, 90, 60),
      'head': (90, 160, 230), 'topcover': (90, 160, 230), 'bottomcover': (90, 160, 230)}


def color(name, bones):
    n = name
    while n:
        if n in HL:
            return HL[n]
        n = bones[n].get('parent')
    return (200, 200, 200)


def render(bones, pose, view, size=360, scale=70, yaw=0.0, label='', eye=None):
    img = Image.new('RGB', (size, size), (24, 24, 30))
    d = ImageDraw.Draw(img)
    proj, depth = VIEWS[view]
    cx, cy = size / 2, size * (0.88 if view != 'top' else 0.5)
    P = lambda p: (cx + proj(p)[0] * scale, cy - proj(p)[1] * scale)
    d.line([P((-2, 0, 0)), P((2, 0, 0))] if view == 'front' else [P((0, 0, -2)), P((0, 0, 2))], fill=(60, 60, 70))
    fwd = (math.sin(math.radians(-yaw)) * 1.5, 0, math.cos(math.radians(yaw)) * 1.5)
    d.line([P((0, 0.02, 0)), P(fwd)], fill=(80, 200, 80), width=2)
    if eye is not None:
        e = P(eye)
        d.ellipse([e[0] - 5, e[1] - 5, e[0] + 5, e[1] + 5], outline=(255, 230, 0), width=2)
    q = quads(bones, pose, yaw)
    q.sort(key=lambda f: sum(depth(p) for p in f[1]) / 4)
    for name, pts in q:
        a, b, c = np.array(pts[0]), np.array(pts[1]), np.array(pts[2])
        n = np.cross(b - a, c - a)
        n = n / (np.linalg.norm(n) + 1e-9)
        light = 0.45 + 0.55 * abs(n @ np.array([0.4, 0.8, 0.45]))
        base = color(name, bones)
        d.polygon([P(p) for p in pts], fill=tuple(int(x * light) for x in base), outline=(20, 20, 20))
    d.text((6, 6), label + ' ' + view, fill=(230, 230, 230))
    return img


def sheet(geo, anim_path, items, out, eye=None):
    rows = []
    for anim, t in items:
        bones, pose = load(geo, anim_path, anim, t)
        rows.append([render(bones, pose, v, label=f'{anim}@{t}', eye=eye) for v in ('front', 'side', 'top')])
    W, H = 360 * 3, 360 * len(rows)
    img = Image.new('RGB', (W, H))
    for i, r in enumerate(rows):
        for j, im in enumerate(r):
            img.paste(im, (j * 360, i * 360))
    img.save(out)


if __name__ == '__main__':
    geo, anim_path, out = sys.argv[1:4]
    items = []
    for a in sys.argv[4:]:
        n, _, t = a.partition('@')
        items.append((n, float(t) if t else None))
    sheet(geo, anim_path, items, out, eye=(0, 1.62, 3.0))
