import json, math, os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageEnhance, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
sys.path.insert(0, os.path.join(ROOT, '..', 'mc_bridge'))
import mcrender as R
from mcrender import Quad

OUT = os.path.join(ROOT, 'preview', 'lore')
LANG = json.load(open(os.path.join(ROOT, 'cavemind', 'src', 'main', 'resources', 'assets', 'cavemind', 'lang', 'vi_vn.json'), encoding='utf-8'))
FONT = 'C:/Windows/Fonts/arialbd.ttf'
M = R.Models()
A = M.a


def tex(tid):
    return A.texture(tid)


def quad(pts, img, uvs=None):
    q = Quad()
    q.pts = np.array(pts, float)
    q.uvs = uvs or [(0, 0), (16, 0), (16, 16), (0, 16)]
    q.tex = img
    n = np.cross(q.pts[3] - q.pts[0], q.pts[1] - q.pts[0])
    q.normal = n / (np.linalg.norm(n) or 1)
    q.shade = True
    q.tint = None
    q.cull = None
    return q


def box(x0, y0, z0, x1, y1, z1, faces):
    out = []
    for d, img in faces.items():
        if img is None:
            continue
        pts = [np.array(p) for p in R.face_corners(d, (x0, y0, z0), (x1, y1, z1))]
        out.append(quad(pts, img))
    return out


def all6(img, top=None, bottom=None):
    return {'north': img, 'south': img, 'east': img, 'west': img, 'up': top or img, 'down': bottom or img}


def fake(name, quads):
    M._baked[name] = (quads, False)
    return name


def sign_tex(wood, lines):
    base = tex('minecraft:entity/signs/' + wood).convert('RGBA')
    board = base.crop((2, 2, 26, 14)).resize((480, 240), Image.NEAREST)
    d = ImageDraw.Draw(board)
    f = ImageFont.truetype(FONT, 34)
    for i, ln in enumerate(lines):
        if not ln:
            continue
        txt, style = ln if isinstance(ln, tuple) else (ln, {})
        w = d.textlength(txt, font=f)
        x, y = (480 - w) / 2, 22 + i * 52
        col = style.get('color', (16, 12, 10))
        d.text((x, y), txt, font=f, fill=col)
        if style.get('strike'):
            d.line((x - 4, y + 21, x + w + 4, y + 21), fill=col, width=4)
    return board


def wall_sign(wood, facing, lines):
    t = sign_tex(wood, lines)
    edge = tex('minecraft:block/' + {'spruce': 'spruce_planks', 'oak': 'oak_planks', 'birch': 'birch_planks'}[wood])
    if facing == 'north':
        q = box(0, 4.5 / 16, 14 / 16, 1, 12.5 / 16, 1, {'north': t, 'east': edge, 'west': edge, 'up': edge, 'down': edge})
    else:
        q = box(0, 4.5 / 16, 0, 1, 12.5 / 16, 2 / 16, {'south': t, 'east': edge, 'west': edge, 'up': edge, 'down': edge})
    return fake('fake:sign_%s_%s_%d' % (wood, facing, abs(hash(str(lines)))), q)


def stand_sign(wood, lines):
    t = sign_tex(wood, lines)
    edge = tex('minecraft:block/oak_planks')
    log = tex('minecraft:block/stripped_oak_log')
    q = box(0, 0.56, 7 / 16, 1, 1.06, 9 / 16, {'north': t, 'south': edge, 'east': edge, 'west': edge, 'up': edge, 'down': edge})
    q += box(7 / 16, 0, 7 / 16, 9 / 16, 0.56, 9 / 16, all6(log))
    return fake('fake:standsign_%d' % abs(hash(str(lines))), q)


def bed(color, part, head_dir):
    wool = tex('minecraft:block/%s_wool' % color)
    white = tex('minecraft:block/white_wool')
    wood = tex('minecraft:block/oak_planks')
    q = box(0, 3 / 16, 0, 1, 9 / 16, 1, {'up': wool, 'north': wool, 'south': wool, 'east': wool, 'west': wool, 'down': wood})
    if part == 'head':
        zs = (0.1, 0.45) if head_dir == 'north' else (0.55, 0.9)
        q += box(0.12, 9 / 16, zs[0], 0.88, 11.5 / 16, zs[1], all6(white))
    q += box(0, 0, 0, 3 / 16, 3 / 16, 3 / 16, all6(wood)) + box(13 / 16, 0, 13 / 16, 1, 3 / 16, 1, all6(wood))
    return fake('fake:bed_%s_%s_%s' % (color, part, head_dir), q)


def chest():
    c = tex('minecraft:entity/chest/normal').convert('RGBA')
    front = Image.new('RGBA', (14, 15))
    front.paste(c.crop((14, 14, 28, 19)), (0, 0))
    front.paste(c.crop((14, 33, 28, 43)), (0, 5))
    top = c.crop((14, 0, 28, 14))
    side = Image.new('RGBA', (14, 15))
    side.paste(c.crop((0, 14, 14, 19)), (0, 0))
    side.paste(c.crop((0, 33, 14, 43)), (0, 5))
    q = box(1 / 16, 0, 1 / 16, 15 / 16, 14 / 16, 15 / 16, {'north': front, 'south': side, 'east': side, 'west': side, 'up': top, 'down': top})
    lock = Image.new('RGBA', (2, 2), (180, 180, 190, 255))
    q += box(7 / 16, 7 / 16, 0, 9 / 16, 11 / 16, 1 / 16, all6(lock))
    return fake('fake:chest', q)


def bell_body():
    g = tex('minecraft:block/gold_block')
    q = box(5 / 16, 6 / 16, 5 / 16, 11 / 16, 13 / 16, 11 / 16, all6(g)) + box(4 / 16, 4 / 16, 4 / 16, 12 / 16, 6 / 16, 12 / 16, all6(g))
    return q


def bell(on_ground):
    q = bell_body() if not on_ground else [x for x in box(0.15, 0, 0.3, 0.85, 0.42, 0.75, all6(tex('minecraft:block/gold_block')))]
    if not on_ground:
        base, _ = M.bake('minecraft:bell[attachment=floor,facing=east,powered=false]')
        q = list(base) + q
    return fake('fake:bell_%s' % on_ground, q)


def skull():
    s = tex('minecraft:entity/skeleton/skeleton').convert('RGBA')
    face = s.crop((8, 8, 16, 16))
    side = s.crop((0, 8, 8, 16))
    top = s.crop((8, 0, 16, 8))
    return fake('fake:skull', box(4 / 16, 0, 4 / 16, 12 / 16, 8 / 16, 12 / 16, {'north': face, 'south': side, 'east': side, 'west': side, 'up': top, 'down': top}))


def stand_child():
    wood = tex('minecraft:block/oak_planks')
    red = Image.new('RGBA', (16, 16), (144, 54, 24, 255))
    head = tex('minecraft:block/oak_planks')
    q = box(0.4, 0, 0.45, 0.47, 0.35, 0.55, all6(wood)) + box(0.53, 0, 0.45, 0.6, 0.35, 0.55, all6(wood))
    q += box(0.36, 0.35, 0.42, 0.64, 0.7, 0.6, all6(red))
    q += box(0.39, 0.72, 0.5, 0.61, 0.94, 0.72, all6(head))
    q += box(0.33, 0.62, 0.62, 0.41, 0.9, 0.74, all6(red)) + box(0.59, 0.62, 0.62, 0.67, 0.9, 0.74, all6(red))
    return fake('fake:standchild', q)


class B:
    def __init__(self):
        self.blocks = {}

    def set(self, x, y, z, st):
        if st is None or st == 'air':
            self.blocks.pop((x, y, z), None)
        else:
            self.blocks[(x, y, z)] = st if ':' in st else 'minecraft:' + st

    def flatten(self, x0, x1, z0, z1, top):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.set(x, 0, z, top)
                for y in range(1, 10):
                    self.set(x, y, z, None)


    def path(self, x0, z0, x1, z1):
        n = max(abs(x1 - x0), abs(z1 - z0))
        for i in range(n + 1):
            x, z = round(x0 + (x1 - x0) * i / n), round(z0 + (z1 - z0) * i / n)
            if self.blocks.get((x, 0, z), '').endswith('grass_block[snowy=false]'):
                self.set(x, 0, z, 'dirt_path')

    def door(self, x, z, wood, facing, open_):
        for y, h in ((1, 'lower'), (2, 'upper')):
            self.set(x, y, z, '%s_door[facing=%s,half=%s,hinge=left,open=%s,powered=false]' % (wood, facing, h, str(open_).lower()))

    def bed(self, x, z, color, head):
        dx, dz = {'south': (0, 1), 'north': (0, -1)}[head]
        self.set(x, 1, z, bed(color, 'foot', head))
        self.set(x + dx, 1, z + dz, bed(color, 'head', head))


GRASS = 'grass_block[snowy=false]'


def terrain(b, rad=22, seed=3):
    rng = np.random.default_rng(seed)
    for x in range(-rad, rad + 1):
        for z in range(-rad, rad + 1):
            b.blocks.setdefault((x, 0, z), 'minecraft:' + GRASS)
            b.blocks.setdefault((x, -1, z), 'minecraft:dirt')
    for _ in range(26):
        x, z = int(rng.integers(-rad + 2, rad - 2)), int(rng.integers(-rad + 2, rad - 2))
        if abs(x) < 9 and abs(z) < 10 or z < 2 and abs(x) < 16:
            continue
        if rng.random() < 0.2:
            b.blocks[(x, 1, z)] = 'minecraft:short_grass' if False else 'minecraft:grass'
            continue
        h = int(rng.integers(4, 7))
        for y in range(1, h + 1):
            b.blocks[(x, y, z)] = 'minecraft:spruce_log[axis=y]'
        for y in range(h - 2, h + 2):
            r = 2 if y < h else 1
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if (dx or dz or y > h) and abs(dx) + abs(dz) <= r + 1:
                        b.blocks.setdefault((x + dx, y, z + dz), 'minecraft:spruce_leaves[distance=1,persistent=true,waterlogged=false]')
    for _ in range(60):
        x, z = int(rng.integers(-rad, rad)), int(rng.integers(-rad, rad))
        if abs(x) < 7 and abs(z) < 9:
            continue
        b.blocks.setdefault((x, 1, z), 'minecraft:grass')


def station(n):
    b = B()
    terrain(b, seed=3)
    b.flatten(-5, 5, -7, 4, GRASS)
    for x in range(-2, 3):
        for z in range(-2, 3):
            b.set(x, 0, z, 'spruce_planks')
            wall, corner = abs(x) == 2 or abs(z) == 2, abs(x) == 2 and abs(z) == 2
            for y in range(1, 4):
                if wall:
                    b.set(x, y, z, 'stripped_spruce_log[axis=y]' if corner else 'spruce_planks')
    for x in range(-3, 4):
        for z in range(-3, 4):
            b.set(x, 4, z, 'spruce_slab[type=bottom,waterlogged=false]')
    if n < 3:
        b.door(0, -2, 'spruce', 'south', True)
    else:
        b.set(0, 1, -2, None)
        b.set(0, 2, -2, None)
    b.set(-2, 2, 0, {1: 'glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]', 2: 'oak_planks', 3: None}[n])
    if n < 3:
        b.bed(1, 0, 'red', 'south')
    else:
        b.bed(4, -6, 'red', 'north')
    if n >= 2:
        b.set(2, 1, 1, None)
    b.set(-1, 1, 1, 'lectern[facing=north,has_book=true,powered=false]')
    b.set(-1, 1, -1, 'barrel[facing=up,open=false]')
    tally = {1: ['IIII I', '', '', ''], 2: ['IIII IIII', 'IIII', '', ''], 3: ['IIII IIII', 'IIII IIII', '', 'II']}[n]
    b.set(0, 2, 1, wall_sign('spruce', 'north', tally))
    b.set(-2, 1, -4, 'stripped_spruce_log[axis=y]')
    b.set(-2, 2, -4, 'stripped_spruce_log[axis=y]')
    if n < 3:
        b.set(-2, 3, -4, bell(False))
    else:
        b.set(-1, 1, -5, bell(True))
    b.set(2, 1, -4, 'spruce_fence[east=false,north=false,south=false,waterlogged=false,west=false]')
    b.set(2, 2, -4, ('lantern' if n < 3 else 'soul_lantern') + '[hanging=false,waterlogged=false]')
    b.set(-4, 1, -6, 'campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]')
    if n == 1:
        b.path(-14, 0, -8, 0)
    else:
        b.path(-16, 0, -3, 0)
    if n == 3:
        b.path(0, -3, 4, -5)
    return b


def L(k):
    return LANG['cavemind.lore.' + k]


def clearing():
    b = B()
    terrain(b, seed=7)
    b.flatten(-5, 6, -5, 5, GRASS)
    for y in range(1, 6):
        b.set(0, y, 0, 'stripped_oak_log[axis=y]')
    b.set(0, 6, 0, 'oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]')
    b.set(1, 4, 0, 'stripped_oak_log[axis=x]')
    b.set(1, 5, 0, 'oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]')
    b.set(-1, 5, 0, 'stripped_oak_log[axis=x]')
    b.set(-1, 6, 0, 'oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]')
    b.set(0, 3, 1, 'stripped_oak_log[axis=z]')
    b.set(0, 4, 1, 'oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]')
    b.set(0, 2, -1, wall_sign('oak', 'north', [L('g.sign%d' % i) for i in range(1, 5)]))
    ring = [(3, 0), (2, 2), (0, 3), (-2, 2), (-3, 0), (-2, -2), (0, -3), (2, -2)]
    for i, (x, z) in enumerate(ring):
        b.set(x, 1, z, 'candle[candles=%d,lit=false,waterlogged=false]' % (1 + i % 3))
    for y in range(1, 4):
        b.set(5, y, 3, 'stripped_birch_log[axis=y]')
    b.set(-3, 1, -4, stand_sign('oak', [L('g.count%d' % i) for i in range(1, 5)]))
    b.set(1, 1, 1, chest())
    b.set(5, 1, 2, stand_child())
    return b


def house():
    b = B()
    terrain(b, seed=11)
    b.flatten(-5, 5, -5, 5, GRASS)
    for x in range(-3, 4):
        for z in range(-2, 4):
            b.set(x, 0, z, 'birch_planks')
            wall, corner = abs(x) == 3 or z in (-2, 3), abs(x) == 3 and z in (-2, 3)
            for y in range(1, 4):
                if wall:
                    b.set(x, y, z, 'stripped_birch_log[axis=y]' if corner else 'birch_planks')
    for x in range(-4, 5):
        for z in range(-3, 5):
            b.set(x, 4, z, 'birch_slab[type=bottom,waterlogged=false]')
    b.door(0, -2, 'birch', 'south', False)
    for x in (-3, 3):
        b.set(x, 2, 0, 'glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]')
    for x in range(-2, 1):
        b.bed(x, 1, 'white', 'south')
    for y in (1, 2, 3):
        b.set(1, y, 2, 'birch_planks')
    b.door(2, 1, 'spruce', 'south', False)
    b.set(2, 1, 2, chest())
    b.set(2, 2, 2, skull())
    names = ['Linh', 'Tuấn', 'Mai', 'Bảo', 'Hà', 'Nam', 'Khoa']
    st = {'strike': True, 'color': (58, 58, 58)}
    b.set(1, 2, -1, wall_sign('birch', 'south', [(n, st) for n in names[:4]]))
    b.set(2, 2, -1, wall_sign('birch', 'south', [(n, st) for n in names[4:]] + [('Bạn', {'color': (90, 15, 15)})]))
    b.set(-2, 1, -1, 'candle[candles=4,lit=false,waterlogged=false]')
    b.set(0, 1, -3, 'candle[candles=1,lit=true,waterlogged=false]')
    return b


def mood(img, night=0.55):
    a = np.asarray(img.convert('RGB'), np.float32) / 255
    h, w = a.shape[:2]
    a = a * np.array([0.78, 0.82, 0.95]) * (1 - night * 0.55)
    yy, xx = np.mgrid[0:h, 0:w]
    v = 1 - 0.55 * (((xx - w / 2) / (w / 1.4)) ** 2 + ((yy - h / 2) / (h / 1.3)) ** 2)
    a = a * np.clip(v, 0.25, 1)[..., None]
    fog = np.array([0.42, 0.45, 0.5])
    a = a * 0.86 + fog * 0.14 * (1 - night * 0.4)
    return Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8))


def shot(blocks, cam, night=0.5, ss=2):
    img, _ = R.render(blocks, cam, M, sky=True, ss=ss)
    return mood(img, night)


def noroof(blocks, y=4):
    return {p: s for p, s in blocks.items() if p[1] < y or abs(p[0]) > 7 or abs(p[2]) > 8}


def site_views(name, b, eye_from, detail, roof_y=4):
    os.makedirs(OUT, exist_ok=True)
    near = {p: s for p, s in b.blocks.items() if abs(p[0]) <= 14 and abs(p[2]) <= 14}
    files = []
    c1 = R.Camera((0.5, 1.5, -0.5), yaw=-35, pitch=30, dist=20, fov=40, w=960, h=600)
    files.append(('Toàn cảnh', shot(near, c1)))
    c2 = R.Camera((0.5, 1.8, 0.5), yaw=eye_from, pitch=3, dist=12, fov=55, w=960, h=600)
    files.append(('Từ chỗ người chơi đi tới', shot(b.blocks, c2, night=0.6)))
    if roof_y:
        c3 = R.Camera((0.5, 1, 0.5), yaw=-30, pitch=58, dist=13, fov=40, w=960, h=600)
        files.append(('Cắt mái, nhìn bên trong', shot(noroof(near, roof_y), c3, night=0.3)))
    tgt, yw, pt, ds = detail
    c4 = R.Camera(tgt, yaw=yw, pitch=pt, dist=ds, fov=45, w=960, h=600)
    files.append(('Cận cảnh', shot(near, c4, night=0.35)))
    out = []
    for i, (cap, im) in enumerate(files):
        fn = '%s_%d.jpg' % (name, i)
        im.save(os.path.join(OUT, fn), quality=88)
        out.append((cap, fn))
    return out


def main():
    want = sys.argv[1:] or ['s1', 'g', 's2', 'h', 's3']
    res = {}
    for k in want:
        if k.startswith('s'):
            n = int(k[1])
            b = station(n)
            det = {1: ((0.5, 2.55, 1.85), 0, 2, 1.35), 2: ((-2.5, 2, 0.5), -90, 8, 4.5), 3: ((4.5, 1.2, -6), -25, 25, 5)}[n]
            res[k] = site_views(k, b, 0, det)
        elif k == 'g':
            res[k] = site_views(k, clearing(), 0, ((0.5, 2.55, -0.2), 0, 3, 1.9), roof_y=0)
        else:
            res[k] = site_views(k, house(), 0, ((2.0, 2.55, -0.9), 180, 3, 2.1))
    json.dump(res, open(os.path.join(OUT, 'index.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(json.dumps(res, ensure_ascii=False))
    if M.missing:
        print('missing', list(M.missing.items())[:12])


main()
