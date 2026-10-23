import json, os, sys, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import viet
R = viet.ROOT
en = json.load(open(os.path.join(R, 'ref', 'mc_en_us.json'), encoding='utf-8'))
vi = json.load(open(os.path.join(R, 'ref', 'mc_vi_vn.json'), encoding='utf-8'))
name = {}
for k, v in en.items():
    if (k.startswith('block.minecraft.') or k.startswith('item.minecraft.')) and k.count('.') == 2 and k in vi:
        name.setdefault(v, vi[k])
w = json.load(open(os.path.join(R, 'work.json'), encoding='utf-8'))
items, where = w['items'], w['where']
done = viet.read_vi()
def low(t):
    return t if t.split()[0] in ('Netherrack','Redstone','Prismarin','Calcit','Netherit') else t[0].lower() + t[1:]
out = []
for i, s in enumerate(items):
    if i in done or where[s][0][0] != 'securitycraft':
        continue
    m = re.fullmatch(r'Reinforced (.+)', s)
    if m and m.group(1) in name:
        out.append('%d\t%s gia cố' % (i, name[m.group(1)])); continue
    m = re.fullmatch(r'(.+) Mine', s)
    if m and m.group(1) in name:
        out.append('%d\tMìn %s' % (i, low(name[m.group(1)]))); continue
open(os.path.join(R, 'batches', 'auto_sc.vi.txt'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(out)); print('\n'.join(out[:40]))
