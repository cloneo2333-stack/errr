import json, math, os, sys

OUT = sys.argv[1]


def hold(bones, L=1.0):
    out = {}
    for b, ch in bones.items():
        out[b] = {}
        for k, v in ch.items():
            out[b][k] = {'0.0': {'vector': list(v)}, f'{L}': {'vector': list(v)}}
    return {'loop': True, 'animation_length': L, 'bones': out}


def ease(t):
    return 0.5 - 0.5 * math.cos(math.pi * max(0.0, min(1.0, t)))


def blend(a, b, L, t0, t1, step=0.125, extra=None):
    names = set(a) | set(b)
    out = {}
    n = int(round(L / step))
    for name in names:
        chans = set(a.get(name, {})) | set(b.get(name, {}))
        out[name] = {}
        for ch in chans:
            va = a.get(name, {}).get(ch, (0, 0, 0) if ch != 'scale' else (1, 1, 1))
            vb = b.get(name, {}).get(ch, (0, 0, 0) if ch != 'scale' else (1, 1, 1))
            keys = {}
            for i in range(n + 1):
                t = round(i * step, 4)
                u = ease((t - t0) / (t1 - t0))
                v = [x + (y - x) * u for x, y in zip(va, vb)]
                if extra and (name, ch) in extra:
                    v = [x + d for x, d in zip(v, extra[(name, ch)](t))]
                keys[f'{t}'] = {'vector': [round(x, 3) for x in v]}
            out[name][ch] = keys
    return {'loop': 'hold_on_last_frame', 'animation_length': L, 'bones': out}


F = lambda x, y=0, z=0: (x, y, z)

damned_fingers = {f'finger{i}': {'rotation': F(6)} for i in range(1, 9)}

still = {'torso': {'rotation': F(4)}, 'neck': {'rotation': F(6)}, 'head': {'rotation': F(-6)},
         'lefthand': {'rotation': F(30, 0, -4)}, 'righthand': {'rotation': F(30, 0, 4)}, 'model': {'position': F(0)}, **damned_fingers}
count = {'torso': {'rotation': F(14)}, 'neck': {'rotation': F(30)}, 'head': {'rotation': F(34, 0, 6)},
         'lefthand': {'rotation': F(42, 0, -6)}, 'righthand': {'rotation': F(42, 0, 6)}, 'model': {'position': F(0, -1, 0)},
         **{f'finger{i}': {'rotation': F(16)} for i in range(1, 9)}}
search_a = {'torso': {'rotation': F(16)}, 'neck': {'rotation': F(8, 32, 0)}, 'head': {'rotation': F(-14, 38, 18)},
            'lefthand': {'rotation': F(20)}, 'righthand': {'rotation': F(38)}, 'model': {'position': F(0, -2, 0)}, **damned_fingers}
search_b = {'torso': {'rotation': F(16)}, 'neck': {'rotation': F(8, -32, 0)}, 'head': {'rotation': F(-14, -38, -18)},
            'lefthand': {'rotation': F(38)}, 'righthand': {'rotation': F(20)}, 'model': {'position': F(0, -2, 0)}, **damned_fingers}
low = {'model': {'position': F(0, -9, 0)}, 'torso': {'rotation': F(38)}, 'neck': {'rotation': F(16)}, 'head': {'rotation': F(-48)},
       'lefthand': {'rotation': F(-18, 0, -10)}, 'righthand': {'rotation': F(-18, 0, 10)}, **{f'finger{i}': {'rotation': F(-6)} for i in range(1, 9)}}
tall = {'model': {'position': F(0, 5, 0)}, 'torso': {'rotation': F(-3)}, 'neck': {'rotation': F(-10)}, 'head': {'rotation': F(4, 0, 24)},
        'lefthand': {'rotation': F(44, 0, -2)}, 'righthand': {'rotation': F(44, 0, 2)}, **{f'finger{i}': {'rotation': F(-4)} for i in range(1, 9)}}
peek = {'model': {'position': F(0, -2, 0)}, 'torso': {'rotation': F(4, 0, -6)}, 'neck': {'rotation': F(2, 0, 26)}, 'head': {'rotation': F(-4, 0, 18)},
        'lefthand': {'rotation': F(40)}, 'righthand': {'rotation': F(5, 0, -15)}, **damned_fingers}
owl_end = {**still, 'neck': {'rotation': F(6, 85, 0)}, 'head': {'rotation': F(-6, 95, 8)}}
point_end = {**still, 'righthand': {'rotation': F(-55, 0, 6)}, 'neck': {'rotation': F(6, 0, 12)}, 'head': {'rotation': F(-8, 0, 6)},
             **{f'finger{i}': {'rotation': F(-8)} for i in range(5, 9)}}

dpath = os.path.join(OUT, 'assets', 'apollyon', 'animations', 'thedamned.animation.json')
dj = json.load(open(dpath, encoding='utf-8'))
A = dj['animations']
A['cm_still'] = hold(still)
A['cm_count'] = hold(count)
A['cm_search_a'] = hold(search_a)
A['cm_search_b'] = hold(search_b)
A['cm_low'] = hold(low)
A['cm_tall'] = hold(tall)
A['cm_peek'] = hold(peek)
twitch = lambda t: (0, 0, 3 * math.sin(t * 9) * math.exp(-(t - 3.6) ** 2 * 6))
A['cm_owl'] = blend(still, owl_end, 4.5, 0.4, 3.8, extra={('head', 'rotation'): twitch})
def keyed(tracks, L, step=0.05, loop='hold_on_last_frame'):
    bones = {}
    n = int(round(L / step))
    for (bone, ch), keys in tracks.items():
        out = {}
        for i in range(n + 1):
            t = round(i * step, 4)
            v = keys[0][1]
            if callable(v):
                v = v(t)
            for (t0, a), (t1, b) in zip(keys, keys[1:]):
                if t0 <= t <= t1:
                    a = a(t) if callable(a) else a
                    b = b(t) if callable(b) else b
                    u = ease((t - t0) / (t1 - t0)) if t1 > t0 else 1
                    v = [x + (y - x) * u for x, y in zip(a, b)]
                    break
                if t > t1:
                    v = b(t) if callable(b) else b
            out[f'{t}'] = {'vector': [round(x, 3) for x in v]}
        bones.setdefault(bone, {})[ch] = out
    return {'loop': loop, 'animation_length': L, 'bones': bones}


def tremble(base, amp, t0, t1, hz=11):
    def f(t):
        if t < t0 or t > t1:
            return list(base)
        k = amp * math.exp(-(t - t0) * 1.2)
        return [base[0] + k * math.sin(t * hz * 1.7), base[1], base[2] + k * math.sin(t * hz)]
    return f


S = lambda b, ch: list(still.get(b, {}).get(ch, (0, 0, 0)))
point = {
    ('torso', 'rotation'): [(0, S('torso', 'rotation')), (3.7, S('torso', 'rotation')), (5.2, [9, 0, 0])],
    ('neck', 'rotation'): [(0, S('neck', 'rotation')), (0.8, [6, 0, 0]), (1.6, [6, 0, 8]), (3.5, [6, 0, 8]), (3.62, [12, 0, 0])],
    ('head', 'rotation'): [(0, S('head', 'rotation')), (0.8, [-6, 0, 0]), (1.6, [-6, 0, 22]), (3.5, [-6, 0, 24]), (3.62, [-2, 0, 0])],
    ('righthand', 'rotation'): [(0, S('righthand', 'rotation')), (1.4, [30, 0, 4]), (3.4, [-36, 4, 0])],
    ('righthand', 'position'): [(0, [0, 0, 0]), (1.4, [0, 0, 0]), (3.4, [-5, 15, -3])],
    ('lefthand', 'rotation'): [(0, S('lefthand', 'rotation')), (5.2, [34, 0, -6])],
    ('finger7', 'rotation'): [(0, [0, 0, 0]), (1.5, [0, 0, 0]), (2.2, [80, 0, 0])],
    ('finger5', 'rotation'): [(0, [0, 0, 0]), (1.8, [0, 0, 0]), (2.5, [80, 0, 0])],
    ('finger8', 'rotation'): [(0, [0, 0, 0]), (2.1, [0, 0, 0]), (2.8, [-80, 0, 0])],
    ('finger6', 'rotation'): [(0, [6, 0, 0]), (1.4, [6, 0, 0]), (2.4, [0, 0, 0])],
    ('mid6', 'rotation'): [(0, [0, 0, 0]), (2.4, [0, 0, 0]), (3.2, [-15, 0, 0])],
    ('thin6', 'rotation'): [(0, [0, 0, 0]), (2.6, [0, 0, 0]), (3.3, [-30, 0, 0]), (3.7, [-30, 0, 0]), (5.2, tremble([-30, 0, 0], 2.2, 3.7, 5.0))],
    ('model', 'position'): [(0, [0, 0, 0]), (5.2, [0, 0, 0])],
}
for i in (1, 2, 3, 4):
    point[(f'finger{i}', 'rotation')] = [(0, [6, 0, 0]), (5.2, [6, 0, 0])]
A['cm_point'] = keyed(point, 5.2)
if 'cm_lean' in A:
    A['cm_lean']['loop'] = 'hold_on_last_frame'


def face(anim):
    m = anim.setdefault('bones', {}).setdefault('model', {})
    r = m.get('rotation')
    if r is None:
        m['rotation'] = {'vector': [0, 90, 0]}
        return
    keys = [r] if 'vector' in r else list(r.values())
    if abs(keys[0]['vector'][1] - 90) < 45:
        return
    for k in keys:
        k['vector'][1] += 90


prof = json.loads(json.dumps(A['animation.theDamned.idle']))
prof['bones'].get('model', {}).pop('rotation', None)
A['cm_profile'] = prof
notice_end = {**still, 'torso': {'rotation': F(9)}, 'neck': {'rotation': F(16, 0, -6)}, 'head': {'rotation': F(-14, 0, -10)},
              'lefthand': {'rotation': F(24, 0, -8)}, 'righthand': {'rotation': F(24, 0, 8)},
              **{f'finger{i}': {'rotation': F(-10)} for i in range(1, 9)}}
jerk = lambda t: (0, 0, 4 * math.sin(t * 14) * math.exp(-(t - 1.1) ** 2 * 30))
A['cm_notice'] = blend(still, notice_end, 3.0, 0.05, 2.4, extra={('head', 'rotation'): jerk})
for name, anim in A.items():
    if name == 'cm_profile':
        continue
    if name.startswith('cm_') or name == 'animation.theDamned.idle':
        face(anim)
json.dump(dj, open(dpath, 'w', encoding='utf-8'), indent=1)

mpath = os.path.join(OUT, 'assets', 'man', 'animations', 'the_man_from_the_fog.animation.json')
mj = json.load(open(mpath, encoding='utf-8'))
M = mj['animations']
stare = {}
for b, ch in M['stare']['bones'].items():
    stare[b] = {}
    for k, v in ch.items():
        first = v['0.0'] if '0.0' in v else v
        stare[b][k] = tuple(first['vector'] if isinstance(first, dict) else first)


def off(base, deltas):
    out = {b: dict(ch) for b, ch in base.items()}
    for b, ch in deltas.items():
        out.setdefault(b, {})
        for k, d in ch.items():
            cur = out[b].get(k, (0, 0, 0))
            out[b][k] = tuple(c + x for c, x in zip(cur, d))
    return out


sniff = off(stare, {
    'torso': {'position': F(0, -13, 0)},
    'body': {'rotation': F(45)}, 'bone': {'rotation': F(20)},
    'neck': {'rotation': F(-40)}, 'bone14': {'rotation': F(-30)},
    'right_leg': {'rotation': F(-70)}, 'bone8': {'rotation': F(80)},
    'left_leg': {'rotation': F(-50)}, 'bone7': {'rotation': F(70)},
    'left_arm': {'rotation': F(-40)}, 'left_arm2': {'rotation': F(-20)},
})
reach = off(stare, {
    'body': {'rotation': F(12)}, 'left_arm2': {'rotation': F(-50, 0, -10)}, 'neck': {'rotation': F(-6)},
})
reach['bone9'] = {'rotation': F(-10, 0, 0)}
M['cm_sniff'] = hold(sniff)
M['cm_reach'] = hold(reach)
M['cm_rise'] = blend(sniff, stare, 3.5, 0.3, 3.2)
json.dump(mj, open(mpath, 'w', encoding='utf-8'), indent=1)
print('damned:', sorted(k for k in A if k.startswith('cm_')))
print('man:', sorted(k for k in M if k.startswith('cm_')))
