import glob, hashlib, json, os, re, shutil, sys, zipfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import snbt
from snbt import Q

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
ORIG = os.path.join(ROOT, 'ftbq_goc')
GAME = r"D:\Minecraft choi chung"
TARGETS = [
    os.path.join(GAME, 'config', 'ftbquests', 'quests', 'chapters'),
    r"E:\Modrinth\profiles\Cave Horror Project 1\config\ftbquests\quests\chapters",
    os.path.join(ROOT, 'dist', 'CaveHorror_TLauncher_1.8', 'game', 'config', 'ftbquests', 'quests', 'chapters'),
]
TIERS = json.load(open(os.path.join(ROOT, 'cavemind', 'src', 'main', 'resources', 'cavemind_tiers.json'), encoding='utf-8'))
TIER = {e: int(t) for t, es in TIERS.items() for e in es}

FINAL = {1: '1CF3AD86D68DF934', 2: '4B32', 3: '6A99', 4: '3A14', 5: '3CDB'}
CHILD = {2: 'Linh', 3: 'Tuấn', 4: 'Mai', 5: 'Bảo', 6: 'Nam'}
KEY = {2: 'cavemind:con_quay_linh', 3: 'cavemind:bi_ve_tuan', 4: 'cavemind:dong_ho_mai', 5: 'cavemind:dieu_giay_bao', 6: 'cavemind:coi_go_nam'}
TITLE = {1: 'Những bộ rễ dõi theo', 2: 'Tiếng gõ bên dưới', 3: 'Nơi bầu trời vỡ ra', 4: 'Sợi chỉ của thời gian vỡ nát', 5: 'Hỗn mang bung xích', 6: 'Tận cùng của vạn vật'}
PLACE = {2: ('nhà kho bên giếng cạn', 4, 'cửa nhà kho, dưới hầm, đáy giếng, gốc cây sau vườn', 'Bật lửa có trong thùng gỗ cạnh bục đọc.'),
         4: ('tháp đồng hồ đổ', 5, 'chân tháp, tầng hai, tầng ba cạnh chuông, dưới đống đá đổ, sau cây đếm', 'Bật lửa có trong thùng gỗ ở chân tháp.'),
         6: ('trường làng trong sương', 6, 'bốn bàn học, bàn cô giáo, tháp chuông ngoài sân', 'Bật lửa có trong thùng gỗ ở sảnh trường.')}
SELF = {3: (6, 'kính ở 4 cạnh, {fire_opal} ở giữa, 4 góc là {nugget}', 'Chỉ chơi khi trời tối, hoặc ở nơi không thấy trời (hang, nhà kín).', 'night'),
        5: (8, 'giấy ở 5 ô, {prism} ở góc trên phải, {powder} ở giữa, dây ở hai góc dưới trái', 'Điểm hẹn phải dựng thật sâu, &edưới độ cao 0&r. Dưới sâu không có ngày đêm.', 'deep')}
UNTITLED = {'730C': 'Vương miện bất tử', '29EC': 'Trái tim bất tử', '6B68': 'Quần giáp bất tử', '3414': 'Bước chân bất tử'}


def qid(name):
    return hashlib.md5(('cavemind-chuong-' + name).encode()).hexdigest()[:16].upper()


def lang_vi():
    out = {}
    pack = os.path.join(GAME, 'resourcepacks', 'Cave Horror Tieng Viet', 'assets')
    for p in glob.glob(os.path.join(pack, '*', 'lang', 'vi_vn.json')) + [os.path.join(ROOT, 'cavemind', 'src', 'main', 'resources', 'assets', 'cavemind', 'lang', 'vi_vn.json')]:
        try:
            out.update(json.load(open(p, encoding='utf-8')))
        except Exception:
            pass
    return out


def drops():
    src = {}
    for jar in glob.glob(os.path.join(GAME, 'mods', '*.jar')):
        try:
            z = zipfile.ZipFile(jar)
        except Exception:
            continue
        for n in z.namelist():
            m = re.match(r'data/([^/]+)/loot_tables/(entities|entity)/(.+)\.json$', n)
            if not m:
                continue
            t = z.read(n).decode('utf-8', 'ignore')
            for it in set(re.findall(r'"name"\s*:\s*"([a-z0-9_]+:[a-z0-9_/]+)"', t)):
                src.setdefault(it, set()).add(m.group(1) + ':' + m.group(3))
    return src


def ename(vi, e):
    ns, p = e.split(':', 1)
    return vi.get('entity.%s.%s' % (ns, p), p.replace('_', ' '))


def iname(vi, i):
    ns, p = i.split(':', 1)
    return vi.get('item.%s.%s' % (ns, p), vi.get('block.%s.%s' % (ns, p), p.replace('_', ' ')))


def full(chs, prefix):
    for d in chs.values():
        for q in d['quests']:
            if q['id'].startswith(prefix):
                return str(q['id'])
    raise KeyError(prefix)


def quest(name, title, x, y, deps, tasks, desc, shape=None, size=None, rewards=None, subtitle=None, hide_deps=False):
    q = {}
    if deps:
        q['dependencies'] = [Q(d) for d in deps]
    if desc:
        q['description'] = [Q(s) for s in desc]
    q['id'] = Q(qid(name))
    if rewards:
        q['rewards'] = rewards
    if shape:
        q['shape'] = Q(shape)
    if size:
        q['size'] = '%sd' % size
    if subtitle:
        q['subtitle'] = Q(subtitle)
    q['tasks'] = tasks
    q['title'] = Q(title)
    q['x'] = '%sd' % x
    q['y'] = '%sd' % y
    return q


def adv(name, a, title):
    return {'advancement': Q('cavemind:chuong/' + a), 'criterion': Q(''), 'id': Q(qid(name + '/t')), 'title': Q(title), 'type': Q('advancement')}


def item(name, it):
    return {'id': Q(qid(name + '/t')), 'item': Q(it), 'type': Q('item')}


RULES = [
    '&lLuật chơi&r',
    '• Khi nó đang đếm: đi trốn, &ckhông được nhìn nó&r',
    '• Khi nó đi tìm: &ckhông đứng chỗ sáng&r, không đánh nó, không bỏ đi quá xa',
    '• Ngồi thụp xuống ở chỗ tối và đứng yên đủ lâu thì thắng',
    '• Phạm luật ba lần: nó săn bạn. Muốn thôi chơi thì rung chuông',
]


def gate_chain(g, prev_final, x0, y0, vi):
    child = CHILD[g]
    qs = []
    y = y0
    cmd = {'auto': Q('enabled'), 'command': Q('/cavemind chuong moi @s %d' % g), 'elevate_perms': 'true', 'id': Q(qid('c%d_moi/r' % g)), 'silent': 'true', 'type': Q('command')}
    check = {'id': Q(qid('c%d_moi/t' % g)), 'title': Q('Nhận lời mời'), 'type': Q('checkmark')}
    if g in PLACE:
        where, n, spots, flint = PLACE[g]
        qs.append(quest('c%d_moi' % g, 'Lời mời của %s' % child, x0, y, [prev_final], [check], [
            'Đứa trẻ thứ tám muốn có người chơi cùng. Lần này nó đợi ở &d%s&r.' % where,
            '',
            '&eBấm vào ô đánh dấu&r để nhận lời mời. Bạn nhận được &dLa bàn sương&r chỉ về điểm hẹn, tọa độ hiện trong khung chat.',
            'Giữa đường có một &dcây đếm cắm biển&r ghi hướng và số khối còn lại.',
            '&8Làm mất la bàn thì vài phút sau nó tự quay lại túi bạn.',
        ], shape='gear', size=1.25, rewards=[cmd], subtitle='Cổng chương %d' % g))
        y += 1.5
        qs.append(quest('c%d_den' % g, 'Tới điểm hẹn', x0, y, [qid('c%d_moi' % g)], [adv('c%d_den' % g, 'c%d_den' % g, 'Đứng trong khu điểm hẹn')], [
            'Đi theo La bàn sương tới &d%s&r. Điểm hẹn chỉ hiện ra khi bạn tới gần.' % where,
            'Mang theo đuốc, đồ ăn và một cái giường: đường khá xa.',
        ], shape='diamond'))
        y += 1.5
        qs.append(quest('c%d_doc' % g, 'Cuốn sổ của %s' % child, x0, y, [qid('c%d_den' % g)], [adv('c%d_doc' % g, 'c%d_doc' % g, 'Mở cuốn sổ trên bục đọc')], [
            'Tìm bục đọc ở điểm hẹn và mở cuốn sổ. Sổ cho biết có mấy ngọn nến và chúng nằm ở đâu.',
        ], shape='square'))
        y += 1.5
        qs.append(quest('c%d_nen' % g, '%d ngọn nến' % n, x0, y, [qid('c%d_doc' % g)], [adv('c%d_nen' % g, 'c%d_nen' % g, 'Thắp đủ nến')], [
            'Thắp đủ &e%d ngọn nến&r: %s.' % (n, spots),
            flint,
            'Thanh hành động sẽ báo bạn đã thắp được mấy ngọn. Nến bị tắt thì thắp lại.',
        ], shape='diamond'))
        y += 1.5
        qs.append(quest('c%d_vat' % g, iname(vi, KEY[g]) if iname(vi, KEY[g]) != KEY[g].split(':')[1].replace('_', ' ') else 'Món đồ chơi của %s' % child, x0, y,
                        [qid('c%d_nen' % g)], [item('c%d_vat' % g, KEY[g])], [
            'Khi ngọn nến cuối cùng cháy, món đồ chơi của %s rơi xuống dưới gốc &dcây đếm&r. Chỉ bạn nhặt được.' % child,
            'Đây là &dvật quan trọng bắt buộc&r để mở chương %d. Làm mất thì quay lại gốc cây đếm, nó sẽ rơi xuống lần nữa.' % g,
        ], shape='rsquare'))
        y += 1.5
        last_dep = qid('c%d_vat' % g)
        how = 'Đợi trời tối. Cầm món đồ chơi của %s và &dnhấp chuột phải vào cây đếm&r ở điểm hẹn.' % child
    else:
        n, recipe, env, _ = SELF[g]
        recipe = recipe.format(fire_opal=iname(vi, 'arphex:fire_opal'), nugget=iname(vi, 'born_in_chaos_v1:dark_metal_nugget'),
                               prism=iname(vi, 'arphex:time_prism_shard'), powder=iname(vi, 'born_in_chaos_v1:phantom_powder'))
        qs.append(quest('c%d_moi' % g, 'Lời mời của %s' % child, x0, y, [prev_final], [check], [
            'Lần này không có nơi nào để đi. %s không có nhà để trốn: ở đâu có cây đếm thì ở đó là sân chơi.' % child,
            '',
            '&eBấm vào ô đánh dấu&r để nhận lời mời và cuốn sổ &dCách dựng điểm hẹn&r.',
        ], shape='gear', size=1.25, rewards=[cmd], subtitle='Cổng chương %d' % g))
        y += 1.5
        qs.append(quest('c%d_vat' % g, iname(vi, KEY[g]) if iname(vi, KEY[g]) != KEY[g].split(':')[1].replace('_', ' ') else 'Món đồ chơi của %s' % child, x0, y,
                        [qid('c%d_moi' % g)], [item('c%d_vat' % g, KEY[g])], [
            'Tự chế tạo ở bàn chế tạo: %s.' % recipe,
            'Đây là &dvật quan trọng bắt buộc&r để mở chương %d. Xem công thức đầy đủ bằng EMI.' % g,
        ], shape='rsquare'))
        y += 1.5
        qs.append(quest('c%d_dung' % g, 'Tự dựng điểm hẹn', x0, y, [qid('c%d_vat' % g)], [adv('c%d_dung' % g, 'c%d_dung' % g, 'Dựng điểm hẹn đủ luật')], [
            'Chọn một chỗ bạn thích và dựng:',
            '• &dCây đếm&r: 3 khúc gỗ chồng lên nhau',
            '• &e%d ngọn nến đang cháy&r quanh gốc, trong 4 khối' % n,
            '• Một cái &echuông&r trong 8 khối',
            '• Một tấm &ebiển ghi tên %s&r trong 5 khối' % child,
            env,
            '',
            'Cầm món đồ chơi và nhấp chuột phải vào cây đếm. Thiếu gì nó sẽ nói, đủ luật thì ván chơi bắt đầu ngay.',
        ], shape='square'))
        y += 1.5
        last_dep = qid('c%d_dung' % g)
        how = 'Cầm món đồ chơi của %s và &dnhấp chuột phải vào cây đếm&r bạn đã dựng.' % child
    qs.append(quest('c%d_mo' % g, 'Ván trốn tìm của %s' % child, x0, y, [last_dep], [adv('c%d_mo' % g, 'c%d_mo' % g, 'Thắng ván trốn tìm')], [
        how,
        'Nó úp mặt vào cây và đếm tới bốn mươi tám.',
        '',
    ] + RULES + [
        '',
        'Thắng: &dmở chương %d&r, nhận &6một vũ khí hiếm&r, và &cnhững loài quái mới bắt đầu xuất hiện&r.' % g,
        'Thua: món đồ chơi vẫn ở chỗ bạn, nghỉ một lát rồi rủ lại.',
        '&8Bạn bè ở gần cùng trốn cũng mở chương nếu họ đã làm đủ các bước trước và mang món đồ chơi của mình.',
        '&8Ván do nó tự tìm tới rủ (bị động) không được tính.',
    ], shape='hexagon', size=1.5, subtitle='Mở chương %d: %s' % (g, TITLE[g])))
    return qs


def free_quests(x, y):
    return [
        quest('tro_tim', 'Tự rủ nó chơi', x, y, [], [adv('tro_tim', 'tro_tim', 'Thắng một ván trốn tìm tự rủ')], [
            'Ở bất kỳ &dcây đếm&r nào (sân chơi bỏ hoang, cây đếm bên đường, điểm hẹn), lúc trời tối, &ecầm một cây nến&r và nhấp chuột phải vào cây.',
            'Muốn chơi gần nhà thì &etự dựng cây đếm&r: 3 khúc gỗ chồng lên nhau và một tấm biển ghi &dCÂY ĐẾM&r trong 5 khối.',
            'Nó sẽ úp mặt vào cây và đếm. Mỗi ngày trong game chỉ rủ được một ván.',
            '',
        ] + RULES + [
            '',
            'Thắng ván đầu tiên chắc chắn có &6vũ khí hiếm&r. Những ván sau có thể là vũ khí hiếm hoặc quà nhỏ.',
            '&8Vũ khí mạnh dần theo chương bạn đang ở.',
        ], shape='heart', subtitle='Trốn tìm chủ động'),
        quest('qua_hiem', 'Quà của đứa trẻ thứ tám', x, y + 1.5, [qid('tro_tim')], [adv('qua_hiem', 'qua_hiem', 'Nhận một vũ khí hiếm')], [
            'Vũ khí hiếm được yểm sẵn, mang tên đứa trẻ đã trao nó. Không quá mạnh, nhưng hiếm.',
        ], shape='heart'),
    ]


def hints(chapter, q, src, vi):
    if q.get('optional') == 'true' or q.get('description'):
        return
    lines = []
    for t in q.get('tasks', []):
        if t['type'] == 'item':
            it = t['item']['id'] if isinstance(t['item'], dict) else t['item']
            ss = sorted(src.get(str(it), []), key=lambda e: (TIER.get(e, 1), e))
            ss = [e for e in ss if TIER.get(e, 1) <= chapter and not e.endswith('_not_despawn')][:3]
            if ss:
                lines.append('Rơi ra từ: ' + ', '.join(ename(vi, e) for e in ss) + '.')
        elif t['type'] == 'kill':
            e = str(t['entity'])
            tier = TIER.get(e, 1)
            if tier > 1:
                lines.append('Loài này chỉ xuất hiện tự nhiên khi đã mở chương %d.' % tier)
    if lines:
        q['description'] = [Q(s) for s in lines]


def validate(chs, src):
    bad = []
    for f, d in chs.items():
        c = int(re.search(r'(\d+)', f).group(1))
        for q in d['quests']:
            if q.get('optional') == 'true':
                continue
            for t in q.get('tasks', []):
                if t['type'] == 'kill':
                    e = str(t['entity'])
                    if TIER.get(e, 1) > c:
                        bad.append('ch%d %s kill %s tier %d' % (c, q.get('title'), e, TIER[e]))
                elif t['type'] == 'item':
                    it = str(t['item']['id'] if isinstance(t['item'], dict) else t['item'])
                    ss = src.get(it)
                    if ss and all(TIER.get(e, 1) > c for e in ss):
                        bad.append('ch%d %s item %s only from %s' % (c, q.get('title'), it, sorted(ss)))
    return bad


def main():
    if not os.path.isdir(ORIG):
        os.makedirs(ORIG)
        for p in glob.glob(os.path.join(TARGETS[0], 'new_chapter_*.snbt')):
            shutil.copy2(p, ORIG)
    vi = lang_vi()
    src = drops()
    chs = {os.path.basename(p): snbt.load(p) for p in sorted(glob.glob(os.path.join(ORIG, 'new_chapter_*.snbt')))}
    finals = {c: full(chs, pre) for c, pre in FINAL.items()}
    for f, d in chs.items():
        c = int(re.search(r'(\d+)', f).group(1))
        for q in d['quests']:
            pre = str(q['id'])[:4]
            if pre in UNTITLED and not q.get('title'):
                q['title'] = Q(UNTITLED[pre])
            if str(q['id']) in finals.values():
                n = [k for k, v in finals.items() if v == str(q['id'])][0]
                q['subtitle'] = Q('Hoàn thành để nhận lời mời chương %d' % (n + 1))
            elif pre == '7005':
                q.pop('subtitle', None)
            hints(c, q, src, vi)
        if c >= 2:
            xs = [float(str(q['x']).rstrip('d')) for q in d['quests']]
            roots = [q for q in d['quests'] if q.get('optional') != 'true' and (not q.get('dependencies') or c == 6 and str(q['id']).startswith('5973'))]
            ry = min(float(str(q['y']).rstrip('d')) for q in roots)
            chain = gate_chain(c, finals[c - 1], min(xs) - 3.5, ry - 1.5 * (6 if c in PLACE else 4) + 1.5, vi)
            mo = qid('c%d_mo' % c)
            for q in roots:
                q['dependencies'] = [Q(x) for x in q.get('dependencies', [])] + [Q(mo)]
            d['quests'] = chain + d['quests']
            d['autofocus_id'] = Q(qid('c%d_moi' % c))
        if c == 1:
            xs = [float(str(q['x']).rstrip('d')) for q in d['quests']]
            d['quests'] = d['quests'] + free_quests(max(xs) + 3.0, 3.5)
    ids = [str(q['id']) for d in chs.values() for q in d['quests']]
    dup = {i for i in ids if ids.count(i) > 1}
    if dup:
        raise SystemExit('duplicate quest ids: %s' % dup)
    alln = set(ids)
    for d in chs.values():
        for q in d['quests']:
            for dep in q.get('dependencies', []):
                if str(dep) not in alln:
                    raise SystemExit('missing dependency %s in %s' % (dep, q.get('title')))
    bad = validate(chs, src)
    for b in bad:
        print('TIER PROBLEM', b)
    if bad and '--force' not in sys.argv:
        raise SystemExit('fix tiers first')
    for f, d in chs.items():
        text = snbt.dumps(d)
        assert snbt.loads(text) == d
        for tdir in TARGETS:
            if not os.path.isdir(tdir):
                continue
            with open(os.path.join(tdir, f), 'w', encoding='utf-8', newline='\n') as out:
                out.write(text + '\n')
    print('quests', len(ids), 'written to', sum(os.path.isdir(t) for t in TARGETS), 'places')


if __name__ == '__main__':
    main()
