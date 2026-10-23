import sys, os, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import viet, json
b = sys.argv[1]
src = [l.split('\t',1)[0] for l in open(os.path.join(viet.BATCH, b+'.txt'), encoding='utf-8') if l.strip()]
out = [l.split('\t',1)[0] for l in open(os.path.join(viet.BATCH, b+'.vi.txt'), encoding='utf-8') if l.strip()]
import viet as V
miss = set(src)-set(out)-set(map(str,V.read_vi())); extra = set(out)-set(src)
print('missing', sorted(miss, key=int)[:30], 'extra', sorted(extra, key=int)[:30])
items = json.load(open(os.path.join(viet.ROOT,'work.json'), encoding='utf-8'))['items']
vi = viet.read_vi(); bad=0
for i in map(int, out):
    s, t = items[i], vi[i]
    a=[x for x in viet.placeholders(s) if not x.startswith('§')]; c=[x for x in viet.placeholders(t) if not x.startswith('§')]
    if a!=c or {x for x in viet.placeholders(s) if x.startswith('§')}!={x for x in viet.placeholders(t) if x.startswith('§')} or re.search(r'\s[-–—]\s|[–—]|\w-\w', viet.PH.sub(' ',t)):
        bad+=1; print(i, '|', s[:80], '|', t[:80])
print('bad', bad, 'total done', len(vi))
