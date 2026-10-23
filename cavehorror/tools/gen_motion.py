import json, math, os, random, sys

DECOMP = sys.argv[1]
OUT = sys.argv[2]

STEP = 0.125


def wave(L, terms, seed):
    rnd = random.Random(seed)
    ph = [rnd.random() * math.tau for _ in terms]

    def f(t):
        return sum(a * math.sin(math.tau * k * t / L + p) for (a, k), p in zip(terms, ph))
    return f


def track(L, fx=None, fy=None, fz=None, base=(0, 0, 0), extra=None):
    keys = {}
    n = int(round(L / STEP))
    for i in range(n + 1):
        t = round(i * STEP, 4)
        v = [base[0] + (fx(t) if fx else 0), base[1] + (fy(t) if fy else 0), base[2] + (fz(t) if fz else 0)]
        keys[t] = v
    if extra:
        for t, d in extra:
            t = round(t, 4)
            keys[t] = [b + dd for b, dd in zip(base, d)] if t not in keys else [k + dd for k, dd in zip(keys[t], d)]
    return {f'{t}': {'vector': [round(x, 3) for x in keys[t]], 'easing': 'linear'} for t in sorted(keys)}


def twitches(L, times, base, amp):
    ev = []
    for t0 in times:
        ev += [(t0, (amp, 0, 0)), (t0 + 0.0625, (-amp * 0.3, 0, 0))]
    return ev


def damned_drift(L, seed, tilt=6.0, sway=2.5, twitch_times=None):
    s = seed
    bones = {}
    bones['model'] = {
        'position': track(L, fy=wave(L, [(1.3, 1), (0.5, 3)], s + 1)),
        'rotation': track(L, fx=wave(L, [(1.2, 2)], s + 2), fz=wave(L, [(sway, 1), (1.0, 2)], s + 3)),
    }
    bones['torso'] = {'rotation': track(L, fx=wave(L, [(2.0, 2), (0.8, 3)], s + 4), base=(3, 0, 0))}
    bones['neck'] = {'rotation': track(L, fx=wave(L, [(3.0, 1)], s + 5), fy=wave(L, [(5.0, 1), (1.5, 3)], s + 6),
                                       fz=wave(L, [(3.0, 1)], s + 7), base=(4, 0, 0))}
    bones['head'] = {'rotation': track(L, fx=wave(L, [(4.0, 2), (1.5, 3)], s + 8), fy=wave(L, [(3.0, 3)], s + 9),
                                       fz=wave(L, [(tilt, 1), (2.0, 2)], s + 10), base=(-4, 0, 0))}
    for side, off in (('lefthand', 11), ('righthand', 31)):
        bones[side] = {
            'rotation': track(L, fx=wave(L, [(6.0, 1), (2.0, 3)], s + off), fz=wave(L, [(2.0, 2)], s + off + 1)),
            'position': track(L, fy=wave(L, [(0.8, 1)], s + off + 2)),
        }
    tt = twitch_times or []
    for i in range(1, 9):
        own = [t for j, t in enumerate(tt) if (j % 8) + 1 == i]
        bones[f'finger{i}'] = {'rotation': track(L, fx=wave(L, [(4.0, 1), (1.5, 2)], s + 50 + i), base=(0.94, 0, 0),
                                                 extra=twitches(L, own, (0.94, 0, 0), -18))}
    return {'loop': True, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


def damned_pause(L=3.5):
    def ease(a, b, t0, t1):
        def f(t):
            if t <= t0: return a
            if t >= t1: return b
            u = (t - t0) / (t1 - t0)
            return a + (b - a) * (0.5 - 0.5 * math.cos(math.pi * u))
        return f
    tilt_in = ease(0, 26, 0.2, 1.6)
    tilt_out = ease(0, -26, 2.4, 3.4)
    bones = {
        'head': {'rotation': track(L, fx=ease(0, 8, 0.2, 1.4), fz=lambda t: tilt_in(t) + tilt_out(t), base=(-4, 0, 0))},
        'neck': {'rotation': track(L, fy=lambda t: ease(0, 12, 0.4, 1.8)(t) + ease(0, -12, 2.4, 3.4)(t), base=(4, 0, 0))},
        'torso': {'rotation': track(L, base=(3, 0, 0))},
        'model': {'position': track(L, fy=wave(L, [(0.6, 1)], 99))},
    }
    for i in range(1, 9):
        bones[f'finger{i}'] = {'rotation': track(L, fx=lambda t, i=i: ease(0, -14, 0.6 + i * 0.05, 2.0)(t) + ease(0, 14, 2.6, 3.4)(t), base=(0.94, 0, 0))}
    return {'loop': False, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


def damned_lean(L=4.0):
    def ease(a, b, t0, t1):
        def f(t):
            if t <= t0: return a
            if t >= t1: return b
            u = (t - t0) / (t1 - t0)
            return a + (b - a) * (0.5 - 0.5 * math.cos(math.pi * u))
        return f
    bones = {
        'torso': {'rotation': track(L, fx=ease(0, 16, 0.2, 2.2), base=(3, 0, 0))},
        'neck': {'rotation': track(L, fx=ease(0, 18, 0.4, 2.4), base=(4, 0, 0))},
        'head': {'rotation': track(L, fx=ease(0, -12, 0.6, 2.6), fz=ease(0, 14, 2.0, 3.8), base=(-4, 0, 0))},
        'model': {'position': track(L, fy=ease(0, -1.5, 0.2, 2.2))},
        'lefthand': {'rotation': track(L, fx=ease(0, -16, 0.8, 2.8))},
        'righthand': {'rotation': track(L, fx=ease(0, -12, 1.0, 3.0))},
    }
    for i in range(1, 9):
        bones[f'finger{i}'] = {'rotation': track(L, fx=ease(0, 10, 1.2 + i * 0.07, 3.2), base=(0.94, 0, 0))}
    return {'loop': False, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


def man_walk(stare, L=2.6, seed=7):
    def base(b, ch='rotation'):
        v = stare[b].get(ch)
        if v is None:
            return (0, 0, 0)
        first = v['0.0'] if '0.0' in v else v
        return tuple(first['vector'] if isinstance(first, dict) else first)

    def limp(k, sharp):
        def f(t):
            u = (t / L) % 1.0
            u = u ** sharp if u < 0.5 else 1 - (1 - u) ** sharp
            return math.sin(math.tau * k * u)
        return f
    bones = {}
    for b in stare:
        bones[b] = {ch: track(L, base=base(b, ch)) for ch in stare[b] if ch in ('rotation', 'position')}
    rl, ll = limp(1, 1.35), limp(1, 0.8)
    bones['right_leg']['rotation'] = track(L, fx=lambda t: 15 * rl(t), base=base('right_leg'))
    bones['left_leg']['rotation'] = track(L, fx=lambda t: -12 * ll(t), base=base('left_leg'))
    bones['bone8']['rotation'] = track(L, fx=lambda t: 12 * max(0, rl(t)), base=base('bone8'))
    bones['bone7']['rotation'] = track(L, fx=lambda t: 10 * max(0, -ll(t)), base=base('bone7'))
    bones['torso']['position'] = track(L, fy=lambda t: 0.7 * abs(rl(t)) - 0.35, base=base('torso', 'position'))
    bones['torso']['rotation'] = track(L, fz=lambda t: 3.5 * rl(t), fy=wave(L, [(2.0, 1)], seed), base=base('torso'))
    bones['left_arm']['rotation'] = track(L, fx=lambda t: 6 * ll(t), base=base('left_arm'))
    bones['left_arm2']['rotation'] = track(L, fx=lambda t: -6 * rl(t), base=base('left_arm2'))
    bones['neck']['rotation'] = track(L, fz=wave(L, [(2.5, 1)], seed + 1), base=base('neck'),
                                      extra=[(1.75, (0, 6, -5)), (1.8125, (0, -3, 2))])
    return {'loop': True, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


dpath = os.path.join(DECOMP, 'apollyon', 'assets', 'apollyon', 'animations', 'thedamned.animation.json')
dj = json.load(open(dpath, encoding='utf-8'))
dj['animations']['cm_drift_a'] = damned_drift(6.0, 11, tilt=6, sway=2.5, twitch_times=[1.4, 2.15, 3.7, 4.4, 5.1])
dj['animations']['cm_drift_b'] = damned_drift(7.5, 23, tilt=9, sway=3.5, twitch_times=[0.9, 2.6, 3.3, 5.8, 6.6, 7.0])
dj['animations']['cm_pause'] = damned_pause()
dj['animations']['cm_lean'] = damned_lean()
dout = os.path.join(OUT, 'assets', 'apollyon', 'animations', 'thedamned.animation.json')
os.makedirs(os.path.dirname(dout), exist_ok=True)
json.dump(dj, open(dout, 'w', encoding='utf-8'), indent=1)

mout = os.path.join(OUT, 'assets', 'man', 'animations', 'the_man_from_the_fog.animation.json')
mj = json.load(open(mout, encoding='utf-8'))
mj['animations']['cm_walk'] = man_walk(mj['animations']['stare']['bones'])
json.dump(mj, open(mout, 'w', encoding='utf-8'), indent=1)
print('damned:', [k for k in dj['animations'] if k.startswith('cm_')], 'man:', [k for k in mj['animations'] if k.startswith('cm_')])
