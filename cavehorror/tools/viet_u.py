import json, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import viet
PFX = sys.argv[1] if len(sys.argv) > 1 else 'u'
CF = re.compile(r'config|option|setting|\.tooltip|gui\.|screen|text\.autoconfig|midnightconfig', re.I)
ADMIN = {'openpartiesandclaims','xaeroworldmap','xaerominimap','xaerobetterpvp','sophisticatedcore','sophisticatedstorage','sophisticatedbackpacks','sound_physics_remastered','modernfix','mapdistancefix','waterdripsound','structure_layout_optimizer','entity_model_features','entity_texture_features','defaultoptions','configured','cloth-config2','chloride','euphoria_patcher','fusion','embeddium','comforts','collective','exposure_catalog','structure_gel','nexuslayer','presencefootsteps','voicechat','creativecore','ftblibrary','obscure_api','citadel','chpcore','iris','sodium'}
w = json.load(open(os.path.join(viet.ROOT, 'work.json'), encoding='utf-8'))
items, where = w['items'], w['where']
done = viet.read_vi()
for f in os.listdir(viet.BATCH):
    if f[0] in 'tuvwxyz' and f.endswith('.txt') and not f.endswith('.vi.txt'):
        os.remove(os.path.join(viet.BATCH, f))
def skip(s):
    ks = where[s]
    return all(ns in ADMIN and CF.search(k) for ns, k in ks)
todo = [i for i in range(len(items)) if i not in done and not skip(items[i])]
todo.sort(key=lambda i: (where[items[i]][0][0], items[i].lower()))
n, cur, chars = 0, [], 0
def flush():
    global n, cur, chars
    n += 1
    open(os.path.join(viet.BATCH, PFX + '%03d.txt' % n), 'w', encoding='utf-8').write('\n'.join(cur) + '\n')
    cur, chars = [], 0
for i in todo:
    s = items[i]
    cur.append('%d\t%s' % (i, s.replace('\r', '').replace('\n', '\\n')))
    chars += len(s)
    if len(cur) >= 300 or chars > 11000:
        flush()
if cur: flush()
print('todo', len(todo), 'batches', n, 'chars', sum(len(items[i]) for i in todo))
