import json, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'viet')
SURVEY = os.path.join(ROOT, 'survey')
BATCH = os.path.join(ROOT, 'batches')
SKIP_NS = {'fancymenu'}

PH = re.compile(r'%(?:\d+\$)?[sdfx%]|%\.\d+f|\{\d+\}|§.|\$\([^)]*\)|\\n|\n|<[^>]+>|\$\{[^}]*\}')


def placeholders(s):
    return sorted(PH.findall(s))


def trivial(v):
    t = PH.sub('', v).strip()
    if not t:
        return True
    if re.fullmatch(r'[\W\d_]+', t):
        return True
    if re.fullmatch(r'https?://\S+', t):
        return True
    return False


def prep(skip=SKIP_NS):
    en = json.load(open(os.path.join(SURVEY, 'en_all.json'), encoding='utf-8'))
    vi = json.load(open(os.path.join(SURVEY, 'vi_all.json'), encoding='utf-8'))
    uniq = {}
    for ns, d in en.items():
        if ns in skip:
            continue
        have = vi.get(ns, {})
        for k, v in d.items():
            if k in have or trivial(v):
                continue
            uniq.setdefault(v, []).append((ns, k))
    items = sorted(uniq.keys(), key=lambda s: (len(s) > 60, s.lower()))
    os.makedirs(BATCH, exist_ok=True)
    json.dump({'items': items, 'where': uniq}, open(os.path.join(ROOT, 'work.json'), 'w', encoding='utf-8'), ensure_ascii=False)
    size, n, cur, chars = 0, 0, [], 0
    for i, s in enumerate(items):
        cur.append('%d\t%s' % (i, s.replace('\n', '\\n')))
        chars += len(s)
        if len(cur) >= 260 or chars > 9000:
            n += 1
            open(os.path.join(BATCH, 'b%03d.txt' % n), 'w', encoding='utf-8').write('\n'.join(cur) + '\n')
            cur, chars = [], 0
    if cur:
        n += 1
        open(os.path.join(BATCH, 'b%03d.txt' % n), 'w', encoding='utf-8').write('\n'.join(cur) + '\n')
    print('unique strings', len(items), 'batches', n, 'words', sum(len(s.split()) for s in items))


def prep2(per=220, maxchars=7000):
    w = json.load(open(os.path.join(ROOT, 'work.json'), encoding='utf-8'))
    items, where = w['items'], w['where']
    done = read_vi()
    for f in os.listdir(BATCH):
        if f.startswith('b') and f.endswith('.txt') and not f.endswith('.vi.txt'):
            os.remove(os.path.join(BATCH, f))
    todo = [i for i in range(len(items)) if i not in done]
    todo.sort(key=lambda i: (where[items[i]][0][0], items[i].lower()))
    n, cur, chars = 0, [], 0
    for i in todo:
        s = items[i]
        cur.append('%d\t%s\t%s' % (i, where[s][0][0], s.replace('\n', '\\n')))
        chars += len(s)
        if len(cur) >= per or chars > maxchars:
            n += 1
            open(os.path.join(BATCH, 't%03d.txt' % n), 'w', encoding='utf-8').write('\n'.join(cur) + '\n')
            cur, chars = [], 0
    if cur:
        n += 1
        open(os.path.join(BATCH, 't%03d.txt' % n), 'w', encoding='utf-8').write('\n'.join(cur) + '\n')
    print('todo', len(todo), 'batches', n)


def read_vi():
    out = {}
    for f in sorted(os.listdir(BATCH)):
        if not f.endswith('.vi.txt'):
            continue
        for line in open(os.path.join(BATCH, f), encoding='utf-8'):
            line = line.rstrip('\n')
            if not line.strip() or '\t' not in line:
                continue
            i, t = line.split('\t', 1)
            out[int(i)] = t.replace('\\n', '\n')
    return out


def check():
    w = json.load(open(os.path.join(ROOT, 'work.json'), encoding='utf-8'))
    items = w['items']
    vi = read_vi()
    bad = 0
    for i, t in sorted(vi.items()):
        src = items[i]
        problems = []
        a = [x for x in placeholders(src) if not x.startswith('§')]
        b = [x for x in placeholders(t) if not x.startswith('§')]
        ca = {x for x in placeholders(src) if x.startswith('§')}
        cb = {x for x in placeholders(t) if x.startswith('§')}
        if a != b or ca != cb:
            problems.append('placeholders %s vs %s' % (placeholders(src), placeholders(t)))
        body = PH.sub(' ', t)
        if re.search(r'\s[-–—]\s|[–—]|\w-\w', body):
            problems.append('hyphen')
        if problems:
            bad += 1
            print('%d: %s | %s | %s' % (i, '; '.join(problems), src[:70], t[:70]))
    print('translated', len(vi), 'of', len(items), 'problems', bad)


def build(out_dir):
    w = json.load(open(os.path.join(ROOT, 'work.json'), encoding='utf-8'))
    items, where = w['items'], w['where']
    vi = read_vi()
    per = {}
    for i, t in vi.items():
        for ns, k in where[items[i]]:
            per.setdefault(ns, {})[k] = t
    extra = os.path.join(ROOT, 'extra')
    if os.path.isdir(extra):
        for f in os.listdir(extra):
            if f.endswith('.json'):
                for ns, d in json.load(open(os.path.join(extra, f), encoding='utf-8')).items():
                    per.setdefault(ns, {}).update(d)
    total = 0
    for ns, d in per.items():
        p = os.path.join(out_dir, 'assets', ns, 'lang')
        os.makedirs(p, exist_ok=True)
        json.dump(dict(sorted(d.items())), open(os.path.join(p, 'vi_vn.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
        total += len(d)
    print('namespaces', len(per), 'keys', total)


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'prep':
        prep()
    elif cmd == 'prep2':
        prep2()
    elif cmd == 'check':
        check()
    elif cmd == 'build':
        build(sys.argv[2])
