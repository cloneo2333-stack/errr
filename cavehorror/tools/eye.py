import sys, os, json, zlib, math, re, glob, base64
from collections import defaultdict

GAME = r'E:\Modrinth\profiles\Cave Horror Project 1'
ROOT = os.path.join(GAME, 'cavemind_eye')
HERE = os.path.dirname(os.path.abspath(__file__))
SEV = {'high': 3, 'med': 2, 'low': 1, 'info': 0}
POS = re.compile(r'BlockPos\{x=(-?\d+), y=(-?\d+), z=(-?\d+)\}')
EDIT_EV = ('left a sign', 'continued a tunnel', 'stripped a tree', 'scheme', 'shrine built', 'carves', 'builds')
STILL_ROLES = ('damned_day',)


def read_gz(path):
    if not os.path.exists(path):
        return []
    raw = open(path, 'rb').read()
    d = zlib.decompressobj(16 + zlib.MAX_WBITS)
    try:
        data = d.decompress(raw)
    except zlib.error:
        data = b''
        d = zlib.decompressobj(16 + zlib.MAX_WBITS)
        for i in range(0, len(raw), 4096):
            try:
                data += d.decompress(raw[i:i + 4096])
            except zlib.error:
                break
    out = []
    for line in data.decode('utf-8', 'replace').splitlines():
        try:
            out.append(json.loads(line))
        except ValueError:
            pass
    return out


def hhalf(fov, asp):
    return math.atan(math.tan(math.radians(fov * 0.5)) * asp)


def project(px, py, pz, yaw, pitch, fov, asp, q):
    yr, xr = math.radians(yaw), math.radians(pitch)
    f = (-math.sin(yr) * math.cos(xr), -math.sin(xr), math.cos(yr) * math.cos(xr))
    r = (f[1] * 0 - f[2] * 1, f[2] * 0 - f[0] * 0, f[0] * 1 - f[1] * 0)
    rl = math.hypot(r[0], r[2]) or 1
    r = (r[0] / rl, 0, r[2] / rl)
    u = (r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0])
    d = (q[0] - px, q[1] - (py + 1.62), q[2] - pz)
    z = sum(a * b for a, b in zip(d, f))
    if z <= 0.05:
        return None
    x = sum(a * b for a, b in zip(d, r))
    y = sum(a * b for a, b in zip(d, u))
    sx = x / z / math.tan(hhalf(fov, asp))
    sy = y / z / math.tan(math.radians(fov * 0.5))
    return sx, sy, math.sqrt(sum(a * a for a in d))


class Tape:
    def __init__(self, d):
        self.dir = d
        self.server = read_gz(os.path.join(d, 'server.jsonl.gz'))
        self.client = read_gz(os.path.join(d, 'client.jsonl.gz'))
        self.start = next((o for o in self.server if o.get('k') == 'start'), {})
        self.frames = defaultdict(list)
        self.events, self.says, self.marks, self.shots, self.maps = [], [], [], [], defaultdict(list)
        for o in self.server:
            k = o.get('k')
            if k == 'f':
                self.frames[o['p']].append(o)
            elif k == 'ev':
                self.events.append(o)
            elif k == 'say':
                self.says.append(o)
            elif k == 'mark':
                self.marks.append(o)
            elif k == 'shot':
                self.shots.append(o)
            elif k == 'map':
                self.maps[o['p']].append(o)
        self.cl = {}
        for o in self.client:
            self.cl[o['g']] = o
        ts = [o['t'] for o in self.server if 't' in o]
        self.t0 = min(ts) if ts else 0
        self.commands = [e['t'] for e in self.events if 'command:' in e.get('s', '')]

    def clock(self, t):
        s = max(0, (t - self.t0) // 20)
        return '%d:%02d' % (s // 60, s % 60)

    def forced(self, t):
        return any(0 <= t - c <= 200 for c in self.commands)

    def near_events(self, t, span=60):
        out = [(e['t'], e['s']) for e in self.events if abs(e['t'] - t) <= span and 'command: eye' not in e['s']]
        out += [(s['t'], '» ' + s['s']) for s in self.says if abs(s['t'] - t) <= span]
        return [x[1] for x in sorted(out)][:14]

    def shot_near(self, t, span=120):
        best = None
        for s in self.shots:
            dt = abs(s['t'] - t)
            if dt <= span and (best is None or dt < best[0]):
                if os.path.exists(os.path.join(self.dir, 'screenshots', s['f'])):
                    best = (dt, s['f'])
        return best[1] if best else None

    def client_at(self, g):
        for k in (g, g - 1, g + 1, g - 2, g + 2):
            if k in self.cl:
                return self.cl[k]
        return None


class Finder:
    def __init__(self, tape):
        self.tape = tape
        self.items = []
        self.stats = defaultdict(lambda: defaultdict(int))

    def add(self, kind, sev, t, player, msg, ent=None, key=None):
        if key:
            for it in self.items:
                if it['key'] == key and abs(it['t'] - t) < 200:
                    it['count'] += 1
                    return
        self.items.append({'kind': kind, 'sev': sev, 't': t, 'p': player, 'msg': msg, 'ent': ent, 'key': key or '', 'count': 1,
                           'forced': self.tape.forced(t)})

    def run(self):
        for p, fr in self.tape.frames.items():
            self.entities(p, fr)
            self.director(p, fr)
            self.perception(p, fr)
            self.hero(p, fr)
            self.overlap(p, fr)
        self.events()
        self.edits()
        self.items.sort(key=lambda i: (-SEV[i['sev']], i['t']))
        return self.items

    def entities(self, p, fr):
        tracks = defaultdict(list)
        for f in fr:
            for e in f.get('ents', []):
                tracks[e['id']].append((f, e))
        for eid, tr in tracks.items():
            role = tr[0][1].get('r', '')
            first_f, first = tr[0]
            if first.get('on') and first.get('vis', 0) >= 0.35 and first['d'] < 90 and role not in ('effigy', 'effigy_morning', 'shrine'):
                self.add('pop_in', 'high', first_f['t'], p, '%s (%s) hiện ra ngay trong tầm nhìn: cách %.0fm, che khuất %.0f%%' % (
                    role, first['ty'], first['d'], (1 - first['vis']) * 100), eid, 'pop_in:' + eid)
            last_f, last = tr[-1]
            nxt = [f for f in fr if f['t'] > last_f['t']][:1]
            if nxt and nxt[0]['t'] - last_f['t'] <= 6 and last.get('on') and last.get('vis', 0) >= 0.35 and last['d'] < 90:
                self.add('pop_out', 'med', last_f['t'], p, '%s (%s) biến mất ngay trước mắt khi đang nhìn thấy rõ (cách %.0fm, %.0f%% lộ)' % (
                    role, last['ty'], last['d'], last['vis'] * 100), eid, 'pop_out:' + eid)
            wall = bad = air = 0
            still_since = None
            for i in range(1, len(tr)):
                (fa, a), (fb, b) = tr[i - 1], tr[i]
                dt = fb['t'] - fa['t']
                dist = math.dist(a['pos'], b['pos'])
                seen = (a.get('on') and a.get('vis', 0) >= 0.2) or (b.get('on') and b.get('vis', 0) >= 0.2)
                if dt <= 4 and dist > 2.5 and seen:
                    self.add('teleport', 'high', fb['t'], p, '%s (%s) nhảy cóc %.1f khối trong %d tick ngay trong tầm nhìn' % (role, b['ty'], dist, dt), eid,
                             'tp:' + eid)
                if role in STILL_ROLES and dt <= 4 and 0.15 < dist <= 2.5 and seen and b['d'] > 3:
                    self.add('moved_seen', 'med', fb['t'], p, '%s cử động %.2f khối trong lúc bạn đang nhìn (luật: không bao giờ cử động khi bị nhìn)' % (role, dist), eid,
                             'mv:' + eid)
                wall = wall + 1 if b.get('wall') or b.get('lava') else 0
                if wall == 10:
                    self.add('embedded', 'high' if b.get('on') else 'med', fb['t'], p, '%s (%s) kẹt trong khối/lava hơn 1 giây tại %s' % (role, b['ty'], fmt(b['pos'])), eid,
                             'wall:' + eid)
                bad = bad + 1 if b.get('bad') and not b.get('wall') else 0
                if bad == 15:
                    self.add('bad_footing', 'med' if b.get('on') else 'low', fb['t'], p, '%s (%s) đứng ở chỗ không đứng được (nửa khối, trong nước, mép...) tại %s' % (
                        role, b['ty'], fmt(b['pos'])), eid, 'bad:' + eid)
                air = air + dt if b.get('bad') and not b.get('gnd') and not b.get('wat') and not b.get('wall') and math.dist(a['pos'][1:2], b['pos'][1:2]) < 0.05 else 0
                if air >= 60 and air - dt < 60:
                    self.add('floating', 'high' if b.get('on') else 'med', fb['t'], p, '%s (%s) lơ lửng giữa không trung %d giây tại %s' % (
                        role, b['ty'], air // 20, fmt(b['pos'])), eid, 'air:' + eid)
                if b.get('nav'):
                    if still_since is None:
                        still_since = (fb['t'], b['pos'])
                    elif math.dist(still_since[1], b['pos']) > 0.8:
                        still_since = (fb['t'], b['pos'])
                    elif fb['t'] - still_since[0] >= 60:
                        self.add('stuck_nav', 'med', fb['t'], p, '%s (%s) đang tìm đường nhưng đứng yên tại chỗ >3 giây ở %s' % (role, b['ty'], fmt(b['pos'])), eid,
                                 'nav:' + eid)
                        still_since = (fb['t'], b['pos'])
                else:
                    still_since = None
                if b['d'] < 1.2 and not (fb.get('man') or {}).get('hunt'):
                    self.add('too_close', 'med', fb['t'], p, '%s (%s) đứng dính vào người bạn (%.1fm)' % (role, b['ty'], b['d']), eid, 'close:' + eid)

    def director(self, p, fr):
        span = None
        for f in fr:
            act = f.get('dir', {}).get('act')
            content = bool(f.get('ents')) or bool((f.get('hero') or {}).get('pos')) or bool((f.get('man') or {}).get('tac'))
            if act and not content and not f.get('sl'):
                if span is None or span[1] != act:
                    span = [f['t'], act]
                elif f['t'] - span[0] >= 900:
                    self.add('idle_act', 'high', f['t'], p, 'Đạo diễn giữ cảnh "%s" suốt %ds mà không có gì xuất hiện (chặn mọi AI khác)' % (act, (f['t'] - span[0]) // 20),
                             None, 'idle:' + act)
                    span[0] = f['t']
            else:
                span = None
        acts = defaultdict(int)
        whys = defaultdict(int)
        for f in fr:
            d = f.get('dir', {})
            acts[d.get('act') or '-'] += 1
            if d.get('why'):
                whys[d['why'].split(' ')[0] + ' ' + ' '.join(d['why'].split(' ')[1:3])] += 1
        self.stats['act_frames'] = acts
        self.stats['blocked'] = whys

    def perception(self, p, fr):
        for slot, label in (('man', 'Man'), ('dmn', 'The Damned'), ('day', 'cảnh ban ngày')):
            run = 0
            for f in fr:
                st = f.get(slot)
                if not st or st.get('seen') or not st.get('body'):
                    run = 0
                    continue
                e = next((e for e in f.get('ents', []) if e['id'] == st['body']), None)
                if e and e.get('on') and e.get('vis', 0) >= 0.6 and abs(e.get('sx', 9)) < 0.35 and abs(e.get('sy', 9)) < 0.45 and not f.get('menu'):
                    run += 1
                    if run == 40:
                        self.add('blind', 'high', f['t'], p, '%s đứng giữa màn hình, lộ rõ %.0f%%, cách %.0fm suốt 4 giây mà AI vẫn chưa ghi nhận là bạn đã thấy' % (
                            label, e['vis'] * 100, e['d']), e['id'], 'blind:' + slot)
                else:
                    run = 0

    def hero(self, p, fr):
        miss = 0
        badrun = 0
        for f in fr:
            h = f.get('hero')
            if not h or not h.get('pos'):
                miss = 0
                badrun = 0
                continue
            badrun = badrun + 1 if h.get('bad') else 0
            if badrun == 10:
                self.add('hero_footing', 'med' if h.get('on') else 'low', f['t'], p, 'Herobrine (%s) đứng 1 giây ở chỗ không đứng được (chìm/lơ lửng) tại %s' % (h['kind'], fmt(h['pos'])),
                         None, 'hbad:' + h['kind'])
            c = self.tape.client_at(f['g'])
            if c is None or c.get('pause') or c.get('scr'):
                continue
            if h.get('on') and h.get('vis', 0) >= 0.5 and h.get('d', 999) < 100 and not h.get('sleep'):
                if not c.get('hero'):
                    miss += 1
                    if miss == 10:
                        self.add('hero_unrendered', 'high', f['t'], p, 'Server tưởng bạn đang thấy Herobrine (%s, %.0fm, lộ %.0f%%) nhưng màn hình không vẽ hắn 1 giây liền' % (
                            h['kind'], h['d'], h['vis'] * 100), None, 'hun:' + h['kind'])
                else:
                    miss = 0
        miss = defaultdict(int)
        for f in fr:
            c = self.tape.client_at(f['g'])
            if c is None or c.get('pause') or c.get('scr'):
                continue
            if 'pr' not in c and 'r' not in c and not f.get('ents'):
                continue
            rendered = set(c.get('r', []))
            present = set(c.get('pr', []))
            for e in f.get('ents', []):
                if e.get('on') and e.get('vis', 0) >= 0.5 and e['d'] < 100 and abs(e.get('sx', 9)) < 0.9 and abs(e.get('sy', 9)) < 0.9:
                    if e['id'] in rendered:
                        miss[e['id']] = 0
                    else:
                        miss[e['id']] += 1
                        if miss[e['id']] == 10:
                            why = 'client không hề có thực thể này (không đồng bộ)' if e['id'] not in present else 'nằm ngoài khung camera thật (server và client lệch hướng nhìn/FOV)'
                            self.add('unrendered', 'high', f['t'], p, 'Server tưởng bạn đang thấy %s (%.0fm, lộ %.0f%%) nhưng màn hình không vẽ: %s' % (
                                e.get('r'), e['d'], e['vis'] * 100, why), e['id'], 'unr:' + e['id'])

    def overlap(self, p, fr):
        for f in fr:
            on = {e.get('r', '').split('_')[0] for e in f.get('ents', []) if e.get('on') and e.get('vis', 0) >= 0.3}
            h = f.get('hero') or {}
            if h.get('on') and h.get('vis', 0) >= 0.3:
                on.add('hero')
            on.discard('')
            if len(on) >= 2:
                self.add('overlap', 'med', f['t'], p, 'Hai thực thể cùng lộ diện một lúc: %s (phá nhịp, loãng nỗi sợ)' % ', '.join(sorted(on)), None, 'ov:' + ','.join(sorted(on)))

    def events(self):
        tries = defaultdict(lambda: [0, 0])
        fails = defaultdict(list)
        unseen = defaultdict(lambda: [0, 0])
        last_tac = defaultdict(list)
        for e in self.tape.events:
            s = e['s']
            m = re.search(r'man (\w+) at BlockPos', s)
            if m:
                tries['man ' + m.group(1)][0] += 1
                tries['man ' + m.group(1)][1] += 1
                lt = last_tac[s.split(' for ')[-1].split(' ')[0]]
                lt.append(m.group(1))
                if len(lt) >= 3 and lt[-1] == lt[-2] == lt[-3] and not self.tape.forced(e['t']):
                    self.add('repeat', 'low', e['t'], '', 'Man dùng chiêu %s 3 lần liền (dễ đoán)' % m.group(1), None, 'rep:' + m.group(1))
            m = re.search(r'no spot for (\w+)', s)
            if m:
                tries['man ' + m.group(1)][0] += 1
                fails['man ' + m.group(1)].append(e)
            m = re.search(r'herobrine (\w+) (begins|could not start)', s)
            if m:
                tries['hero ' + m.group(1)][0] += 1
                if m.group(2) == 'begins':
                    tries['hero ' + m.group(1)][1] += 1
                else:
                    fails['hero ' + m.group(1)].append(e)
            m = re.search(r'haunting (\w+) (begins|could not start)', s)
            if m:
                tries['damned ' + m.group(1)][0] += 1
                if m.group(2) == 'begins':
                    tries['damned ' + m.group(1)][1] += 1
                else:
                    fails['damned ' + m.group(1)].append(e)
            m = re.search(r'daylight (\w+) (begins|could not begin)', s)
            if m:
                tries['day ' + m.group(1)][0] += 1
                if m.group(2) == 'begins':
                    tries['day ' + m.group(1)][1] += 1
                else:
                    fails['day ' + m.group(1)].append(e)
            m = re.search(r'herobrine (\w+) ends .*\(seen=(true|false)\)', s)
            if m:
                unseen['hero ' + m.group(1)][0] += 1
                unseen['hero ' + m.group(1)][1] += m.group(2) == 'false'
            m = re.search(r'the Man leaves \S+ \(seen (\d+)', s)
            if m:
                unseen['man session'][0] += 1
                unseen['man session'][1] += m.group(1) == '0'
            if 'gives up' in s:
                self.add('gave_up', 'med', e['t'], '', 'Man bỏ cả phiên vì không chiêu nào hợp địa hình: ' + s, None, 'giveup')
            if 'failed' in s and 'spot search' not in s:
                self.add('exception', 'high', e['t'], '', 'Lỗi chạy: ' + s, None, 'exc:' + s[:40])
        for k, (n, ok) in tries.items():
            if n >= 3 and ok / n < 0.34:
                why = defaultdict(int)
                for e in fails[k]:
                    near = [x for x in self.tape.events if 0 <= e['t'] - x['t'] <= 1 and 'spot search failed' in x['s']]
                    for x in near:
                        g = dict(re.findall(r'(\w+)=(-?\d+)', x['s']))
                        tot = max(1, int(g.get('ground', 1)))
                        if int(g.get('bright', 0)) >= tot * 0.8:
                            why['quá sáng'] += 1
                        elif int(g.get('avgvis', 100)) < 25:
                            why['địa hình che khuất'] += 1
                        elif int(g.get('onscreen', 0)) >= tot * 0.6:
                            why['mọi chỗ đều nằm trong tầm nhìn'] += 1
                        elif tot < int(g.get('tries', 1)) * 0.5:
                            why['không có chỗ đứng'] += 1
                        else:
                            why['điều kiện riêng của chiêu'] += 1
                reason = ', '.join('%s ×%d' % kv for kv in sorted(why.items(), key=lambda kv: -kv[1])) or 'không rõ'
                self.add('setup_fail', 'high' if n >= 5 else 'med', fails[k][-1]['t'] if fails[k] else 0, '', '%s dựng thất bại %d/%d lần (%s)' % (k, n - ok, n, reason), None,
                         'sf:' + k)
        self.stats['tries'] = {k: v for k, v in tries.items()}
        self.stats['unseen'] = {k: v for k, v in unseen.items()}

    def edits(self):
        for e in self.tape.events:
            s = e['s']
            if not any(k in s for k in EDIT_EV):
                continue
            m = POS.search(s)
            if not m:
                continue
            q = [int(v) + 0.5 for v in m.groups()]
            for p, fr in self.tape.frames.items():
                if p not in s:
                    continue
                at = (e.get('at') or {}).get(p)
                if at is not None:
                    if at.get('on') and at.get('clear') and at.get('d', 999) < 96:
                        self.add('edit_in_view', 'low' if self.tape.forced(e['t']) else 'high', e['t'], p,
                                 'Sửa thế giới ngay trước mắt, không bị gì che (cách %.0fm): %s' % (at['d'], s), None, 'edit:' + s[:30])
                    continue
                f = min(fr, key=lambda f: abs(f['t'] - e['t']), default=None)
                if f is None or abs(f['t'] - e['t']) > 40:
                    continue
                pr = project(*f['pos'], f['yr'], f['xr'], f.get('fov', 70), f.get('asp', 1.78), q)
                if pr and abs(pr[0]) <= 1 and abs(pr[1]) <= 1 and pr[2] < 80:
                    self.add('edit_in_view', 'low' if self.tape.forced(e['t']) else 'high', e['t'], p,
                             'Sửa thế giới ngay trong khung hình (cách %.0fm, chưa xét vật cản): %s' % (pr[2], s), None, 'edit:' + s[:30])


def fmt(v):
    return '%.0f %.0f %.0f' % tuple(v)


def pick_dir(arg):
    if arg and os.path.isdir(arg):
        return arg
    runs = sorted(glob.glob(os.path.join(ROOT, '*')))
    if not runs:
        raise SystemExit('no recordings in ' + ROOT)
    if arg:
        m = [r for r in runs if arg in os.path.basename(r)]
        if m:
            return m[-1]
    return runs[-1]


def slim_frames(fr):
    out = []
    for f in fr:
        o = {'t': f['t'], 'g': f['g'], 'tod': f.get('tod'), 'pos': f['pos'], 'yr': f['yr'], 'xr': f['xr'], 'fov': f.get('fov'), 'asp': f.get('asp'),
             'env': f.get('env'), 'li': f.get('li'), 'dir': f.get('dir')}
        for k in ('man', 'dmn', 'hero', 'day', 'ents', 'ex', 'sl', 'menu'):
            if k in f:
                o[k] = f[k]
        out.append(o)
    return out


def build_html(tape, items, stats):
    data = {'dir': os.path.basename(tape.dir), 'start': tape.start, 't0': tape.t0, 'players': {}, 'items': items,
            'events': [[e['t'], e['s']] for e in tape.events] + [[s['t'], '» ' + s['s']] for s in tape.says] + [[m['t'], '★ ' + m['s']] for m in tape.marks],
            'shots': [[s['t'], s['f'], s.get('tag', '')] for s in tape.shots if os.path.exists(os.path.join(tape.dir, 'screenshots', s['f']))],
            'stats': {k: dict(v) for k, v in stats.items()}}
    data['events'].sort()
    for p, fr in tape.frames.items():
        maps = []
        for m in tape.maps.get(p, []):
            if maps and m['t'] - maps[-1]['t'] < 1200 and abs(m['cx'] - maps[-1]['cx']) < 20 and abs(m['cz'] - maps[-1]['cz']) < 20 and abs(m['cy'] - maps[-1]['cy']) < 6:
                continue
            maps.append({'t': m['t'], 'cx': m['cx'], 'cy': m['cy'], 'cz': m['cz'], 'r': m['r'], 'col': m['col'], 'hgt': m['hgt'], 'cave': m['cave']})
        rendered = {}
        for f in fr:
            c = tape.client_at(f['g'])
            if c:
                rendered[f['t']] = {'r': c.get('r', []), 'pr': c.get('pr', []), 'h': bool(c.get('hero')), 'third': c.get('third', False), 'scr': c.get('scr', '')}
        data['players'][p] = {'frames': slim_frames(fr), 'maps': maps, 'client': rendered}
    for it in items:
        it['near'] = tape.near_events(it['t'])
        it['shot'] = tape.shot_near(it['t'])
        it['clock'] = tape.clock(it['t'])
    tpl = open(os.path.join(HERE, 'eye_view.html'), encoding='utf-8').read()
    html = tpl.replace('/*DATA*/null', json.dumps(data, ensure_ascii=False, separators=(',', ':')))
    out = os.path.join(tape.dir, 'eye.html')
    open(out, 'w', encoding='utf-8').write(html)
    return out


def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else ''
    d = pick_dir(arg)
    tape = Tape(d)
    nf = sum(len(v) for v in tape.frames.values())
    print('recording:', d)
    print('mod:', tape.start.get('mod'), '| frames:', nf, '| client frames:', len(tape.client), '| events:', len(tape.events), '| shots:', len(tape.shots))
    fd = Finder(tape)
    items = fd.run()
    by = defaultdict(int)
    for it in items:
        by[(it['sev'], it['kind'])] += it['count']
    print('\nfindings:')
    for (sev, kind), n in sorted(by.items(), key=lambda kv: (-SEV[kv[0][0]], -kv[1])):
        print('  %-5s %-16s x%d' % (sev, kind, n))
    print('\ntop:')
    for it in items[:40]:
        print('  [%s] %s %s%s %s' % (it['sev'], tape.clock(it['t']), it['kind'], ' (lệnh thử)' if it['forced'] else '', it['msg'][:170] + (' ×%d' % it['count'] if it['count'] > 1 else '')))
    st = fd.stats
    if st.get('tries'):
        print('\nsetup success:')
        for k, (n, ok) in sorted(st['tries'].items()):
            print('  %-22s %d/%d' % (k, ok, n))
    if st.get('unseen'):
        print('\nended unseen:')
        for k, (n, u) in sorted(st['unseen'].items()):
            print('  %-22s %d/%d' % (k, u, n))
    if st.get('blocked'):
        print('\ndirector blocked (frames):')
        for k, n in sorted(st['blocked'].items(), key=lambda kv: -kv[1])[:8]:
            print('  %-40s %d' % (k, n))
    out = build_html(tape, items, st)
    print('\nreport:', out)


if __name__ == '__main__':
    main()
