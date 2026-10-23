import json, os, sys, copy

SRC = sys.argv[1]
OUT = sys.argv[2]

j = json.load(open(SRC, encoding='utf-8'))
anims = j['animations']
stare = anims['stare']['bones']


def base(b):
    v = stare[b]
    out = {}
    for ch, keys in v.items():
        first = keys['0.0'] if '0.0' in keys else keys
        vec = first['vector'] if isinstance(first, dict) else first
        out[ch] = vec
    return out


def held(length):
    bones = {}
    for b in stare:
        bones[b] = {ch: {'0.0': {'vector': vec}, f'{length}': {'vector': vec}} for ch, vec in base(b).items()}
    return bones


def add(v, d):
    return [round(v[i] + d[i], 4) for i in range(3)]


def tilt():
    L = 3.4
    bones = held(L)
    n = base('neck')['rotation']
    b14 = base('bone14')['rotation']
    h = base('h_head')['rotation']
    bones['neck']['rotation'] = {
        '0.0': {'vector': n},
        '0.9': {'vector': n},
        '2.8': {'vector': add(n, [4, 0, -16]), 'easing': 'easeInOutSine'},
        f'{L}': {'vector': add(n, [4, 0, -16])}}
    bones['bone14']['rotation'] = {
        '0.0': {'vector': b14},
        '1.1': {'vector': b14},
        '2.9': {'vector': add(b14, [-6, -10, 12]), 'easing': 'easeInOutSine'},
        f'{L}': {'vector': add(b14, [-6, -10, 12])}}
    bones['h_head']['rotation'] = {
        '0.0': {'vector': h},
        '1.2': {'vector': h},
        '3.0': {'vector': add(h, [8, 4, 44]), 'easing': 'easeInOutSine'},
        f'{L}': {'vector': add(h, [8, 4, 44])}}
    bones['h_jaw'] = {
        'position': {'0.0': {'vector': [0, 0, 0]}, '2.4': {'vector': [0, 0, 0]},
                     f'{L}': {'vector': [0, -1.6, 0.4], 'easing': 'easeInQuad'}},
        'scale': {'0.0': {'vector': [1, 1, 1]}, f'{L}': {'vector': [1, 1.25, 1], 'easing': 'easeInQuad'}}}
    return {'loop': 'hold_on_last_frame', 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


def twitch():
    L = 0.55
    bones = held(L)
    n = base('neck')['rotation']
    b14 = base('bone14')['rotation']
    h = base('h_head')['rotation']
    jit = [(0.0, [0, 0, 0]), (0.06, [3, -14, 12]), (0.11, [-2, 10, -9]), (0.17, [2, -18, 14]), (0.24, [0, 6, -4]), (0.3, [1, -9, 7]), (L, [0, 0, 0])]
    bones['neck']['rotation'] = {f'{t}': {'vector': add(n, [d[0], d[1] * 0.6, d[2] * 0.6])} for t, d in jit}
    bones['bone14']['rotation'] = {f'{t}': {'vector': add(b14, [d[0], d[1], d[2]])} for t, d in jit}
    bones['h_head']['rotation'] = {f'{t}': {'vector': add(h, [d[0] * 2, d[1] * 1.4, d[2] * 1.4])} for t, d in jit}
    return {'loop': False, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


def hold():
    L = 4.0
    bones = held(L)
    return {'loop': True, 'animation_length': L, 'override_previous_animation': True, 'bones': bones}


anims['cm_tilt'] = tilt()
anims['cm_twitch'] = twitch()
anims['cm_hold'] = hold()
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(j, open(OUT, 'w', encoding='utf-8'), indent=1)
print('animations:', list(anims))
