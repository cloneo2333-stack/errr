import json, os, re, sys, collections

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'viet')
w = json.load(open(os.path.join(ROOT, 'work.json'), encoding='utf-8'))
items, where = w['items'], w['where']

COLORS = {'White': 'trắng', 'Orange': 'cam', 'Magenta': 'đỏ tươi', 'Light Blue': 'xanh nước biển nhạt', 'Yellow': 'vàng', 'Lime': 'xanh lá mạ',
          'Pink': 'hồng', 'Gray': 'xám', 'Light Gray': 'xám nhạt', 'Cyan': 'xanh lơ', 'Purple': 'tím', 'Blue': 'xanh dương', 'Brown': 'nâu',
          'Green': 'xanh lá', 'Red': 'đỏ', 'Black': 'đen'}
WOOD = {'Oak': 'gỗ sồi', 'Spruce': 'gỗ vân sam', 'Birch': 'gỗ bạch dương', 'Jungle': 'gỗ rừng', 'Acacia': 'gỗ xiêm gai', 'Dark Oak': 'gỗ sồi sẫm',
        'Mangrove': 'gỗ đước', 'Cherry': 'gỗ anh đào', 'Bamboo': 'tre', 'Crimson': 'gỗ đỏ thẫm', 'Warped': 'gỗ kì dị', 'Pale Oak': 'gỗ sồi nhạt',
        'Fir': 'gỗ linh sam', 'Redwood': 'gỗ đỏ', 'Mahogany': 'gỗ dái ngựa', 'Jacaranda': 'gỗ phượng tím', 'Palm': 'gỗ dừa', 'Willow': 'gỗ liễu',
        'Dead': 'gỗ chết', 'Magic': 'gỗ ma thuật', 'Umbran': 'gỗ phù thủy', 'Hellbark': 'gỗ Hellbark', 'Empyreal': 'gỗ thiên giới', 'Maple': 'gỗ phong',
        'Pine': 'gỗ thông'}
STONE = {'Andesite': 'đá andesit', 'Diorite': 'đá diorit', 'Granite': 'đá hoa cương', 'Stone Bricks': 'gạch đá', 'Stone Brick': 'gạch đá',
         'Sandstone': 'cát kết', 'Red Sandstone': 'cát kết đỏ', 'Blackstone': 'đá đen', 'Deepslate': 'đá bảng sâu', 'Mud Bricks': 'gạch bùn',
         'Mud Brick': 'gạch bùn', 'Prismarine': 'Prismarin', 'Prismarine Bricks': 'gạch Prismarin', 'End Stone Bricks': 'gạch đá End',
         'End Stone': 'đá End', 'Nether Bricks': 'gạch Nether', 'Red Nether Bricks': 'gạch Nether đỏ', 'Cobblestone': 'đá cuội',
         'Mossy Cobblestone': 'đá cuội phủ rêu', 'Stone': 'đá', 'Quartz': 'thạch anh', 'Purpur': 'purpur', 'Bricks': 'gạch', 'Brick': 'gạch',
         'Polished Andesite': 'đá andesit đánh bóng', 'Polished Diorite': 'đá diorit đánh bóng', 'Polished Granite': 'đá hoa cương đánh bóng',
         'Polished Blackstone': 'đá đen đánh bóng', 'Polished Deepslate': 'đá bảng sâu đánh bóng', 'Deepslate Bricks': 'gạch đá bảng sâu',
         'Deepslate Tile': 'ngói đá bảng sâu', 'Mossy Stone Brick': 'gạch đá phủ rêu', 'Mossy Stone Bricks': 'gạch đá phủ rêu',
         'Smooth Stone': 'đá mịn', 'Smooth Sandstone': 'cát kết mịn', 'Dark Prismarine': 'Prismarin sẫm', 'Tuff': 'đá túp', 'Calcite': 'calcit',
         'Metal': 'kim loại', 'Iron': 'sắt', 'Glass': 'kính', 'Thatch': 'rạ', 'Rope': 'dây thừng', 'Log': 'gỗ súc'}
MAT = {}
MAT.update(WOOD)
MAT.update(STONE)
for c, v in COLORS.items():
    MAT[c + ' Concrete'] = 'bê tông ' + v
    MAT[c + ' Terracotta'] = 'đất nung ' + v
    MAT[c + ' Wool'] = 'len ' + v
    MAT[c] = v
MAT['Terracotta'] = 'đất nung'
MAT.update({'Prismarine Brick': 'gạch Prismarin', 'Deepslate Brick': 'gạch đá bảng sâu', 'Blackstone Brick': 'gạch đá đen',
            'Dark Prismarine Brick': 'gạch Prismarin sẫm', 'Bamboo Mosaic': 'khảm tre', 'Iron Bars': 'song sắt'})
for k, v in list(WOOD.items()):
    MAT[k + ' Planks'] = 'ván ' + v
    MAT[k + ' Log'] = 'thân cây ' + v.replace('gỗ ', '')
    MAT[k + ' Stem'] = 'cuống ' + v.replace('gỗ ', '')
    MAT[k + ' Bark'] = 'vỏ ' + v
NAMES = sorted(MAT, key=len, reverse=True)
LEAD = {'Rectangle': 'chữ nhật', 'Resizeable': 'co giãn'}


def parse(s):
    words = s
    lead = None
    for k in LEAD:
        if words.startswith(k + ' '):
            lead = k
            words = words[len(k) + 1:]
    stripped = False
    if words.startswith('Rope '):
        rest = words[5:]
        for m in sorted(WOOD, key=len, reverse=True):
            if rest.startswith(m + ' '):
                return lead, False, m, 'Rope ' + rest[len(m):].strip()
    if words.startswith('Stripped '):
        stripped = True
        words = words[9:]
    for m in NAMES:
        if words == m or words.startswith(m + ' '):
            return lead, stripped, m, words[len(m):].strip()
    return lead, stripped, None, words


if __name__ == '__main__':
    mode = sys.argv[1]
    mcw = [s for s in items if all(ns.startswith('mcw') for ns, k in where[s])]
    if mode == 'objects':
        objs = collections.Counter()
        unmatched = []
        for s in mcw:
            lead, st, m, obj = parse(s)
            if m is None:
                unmatched.append(s)
            else:
                objs[(lead or '') + '|' + obj] += 1
        for o, n in sorted(objs.items()):
            print('%s\t%d' % (o, n))
        print('---unmatched', len(unmatched))
        for u in unmatched:
            print(u)
    elif mode == 'compose':
        objs = json.load(open(os.path.join(ROOT, 'mcw_objects.json'), encoding='utf-8'))
        out = {}
        miss = []
        for s in mcw:
            lead, st, m, obj = parse(s)
            key = (lead or '') + '|' + obj
            if m is None or key not in objs:
                miss.append(s)
                continue
            head = objs[key]
            mat = MAT[m] + (' đã cạo vỏ' if st else '')
            if '{-}' in head:
                t = head.replace('{-}', '')
            elif '{w}' in head:
                t = head.replace('{w}', mat.replace('gỗ ', '').replace('tre', 'tre'))
            else:
                t = head.replace('{m}', mat) if '{m}' in head else (head + ' ' + mat if head else mat)
            t = re.sub(r'\s+', ' ', t).strip()
            out[s] = t[0].upper() + t[1:]
        lines = []
        idx = {s: i for i, s in enumerate(items)}
        for s, t in out.items():
            lines.append('%d\t%s' % (idx[s], t))
        open(os.path.join(ROOT, 'batches', 'mcw.vi.txt'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
        print('composed', len(out), 'missing', len(miss))
        for s in miss:
            print(s)
