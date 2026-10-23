import json, os, re, sys, zipfile

MODS = sys.argv[1]
OUT = sys.argv[2]
os.makedirs(OUT, exist_ok=True)


def load(raw):
    txt = raw.decode('utf-8-sig', errors='replace')
    try:
        return json.loads(txt)
    except Exception:
        txt = re.sub(r'//.*', '', txt)
        txt = re.sub(r',\s*([}\]])', r'\1', txt)
        try:
            return json.loads(txt)
        except Exception:
            return None


rows = []
allen = {}
allvi = {}
for f in sorted(os.listdir(MODS)):
    if not f.endswith('.jar'):
        continue
    try:
        z = zipfile.ZipFile(os.path.join(MODS, f))
    except Exception:
        continue
    names = z.namelist()
    en = [n for n in names if re.match(r'assets/[^/]+/lang/en_us\.json$', n, re.I)]
    for n in en:
        ns = n.split('/')[1]
        d = load(z.read(n))
        if not isinstance(d, dict):
            continue
        d = {k: v for k, v in d.items() if isinstance(v, str)}
        vin = n[:-len('en_us.json')] + 'vi_vn.json'
        vi = {}
        if vin in names:
            v = load(z.read(vin))
            if isinstance(v, dict):
                vi = {k: x for k, x in v.items() if isinstance(x, str)}
        missing = {k: v for k, v in d.items() if k not in vi and v.strip()}
        words = sum(len(v.split()) for v in missing.values())
        rows.append((f, ns, len(d), len(vi), len(missing), words))
        key = ns
        allen.setdefault(key, {}).update(d)
        allvi.setdefault(key, {}).update(vi)
json.dump(allen, open(os.path.join(OUT, 'en_all.json'), 'w', encoding='utf-8'), ensure_ascii=False)
json.dump(allvi, open(os.path.join(OUT, 'vi_all.json'), 'w', encoding='utf-8'), ensure_ascii=False)
rows.sort(key=lambda r: -r[4])
tot = sum(r[4] for r in rows)
totw = sum(r[5] for r in rows)
print('namespaces', len(rows), 'missing strings', tot, 'missing words', totw)
for r in rows[:80]:
    print('%-60s %-28s en=%5d vi=%5d miss=%5d words=%6d' % r)
