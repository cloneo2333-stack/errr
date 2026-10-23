import json, os
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'viet')
w = json.load(open(os.path.join(R, 'work.json'), encoding='utf-8'))
M = {'Adamantite':'Adamantit','Netherite':'Netherit','Diamond':'kim cương','Iron':'sắt','Stone':'đá','Wooden':'gỗ','Golden':'vàng','Electrum':'Electrum','Silver':'bạc','Necromium':'Necromium'}
T = {'Battle Axe':'Rìu chiến','Dagger':'Dao găm','Spear':'Giáo','Stiletto':'Dao nhọn','Rapier':'Kiếm liễu','Katana':'Katana','Greatsword':'Đại kiếm','Shortsword':'Đoản kiếm','Longsword':'Trường kiếm','Twinblade':'Song nhận','Zweihander':'Kiếm hai tay','Pike':'Giáo dài','Glaive':'Đại đao','Halberd':'Kích','Throwing Knife':'Dao ném','Shuriken':'Phi tiêu','Chakram':'Vòng phi đao','Javelin':'Lao'}
out = []
for i, s in enumerate(w['items']):
    p = s.split(' ', 1)
    if len(p) == 2 and p[0] in M and p[1] in T and w['where'][s][0][0] == 'dixtas_armory':
        out.append('%d\t%s %s' % (i, T[p[1]], M[p[0]]))
open(os.path.join(R, 'batches', 'auto_armory.vi.txt'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(out))
