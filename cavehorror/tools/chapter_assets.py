import json, math, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'cavemind', 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'cavemind')
DATA = os.path.join(RES, 'data', 'cavemind')


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def grid(rows, pal):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), hexc(pal[ch]))
    return img


def spinning_top():
    rows = [
        "................",
        ".......oo.......",
        "......oHHo......",
        "......oHho......",
        "....ooRRRRoo....",
        "...oRRrrrrRRo...",
        "..oYYYYYYYYYYo..",
        "..oRRRRRRRRRRo..",
        "..orrrrrrrrrro..",
        "...oYYYYYYYYo...",
        "....oRRRRRRo....",
        ".....orrrro.....",
        "......oMMo......",
        ".......mo.......",
        "................",
        "................",
    ]
    return grid(rows, {'o': '3a1d12', 'H': '9a6a35', 'h': '6b4423', 'R': 'c23b2e', 'r': '86231b', 'Y': 'e8bd45', 'M': 'd0d0d0', 'm': '7a7a7a'})


def marble():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 7.5, 6.3
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            if d > r - 1.0:
                img.putpixel((x, y), hexc('163a46'))
                continue
            a = math.atan2(dy, dx)
            swirl = math.sin(a * 2 + d * 0.9)
            t = d / r
            base = (int(60 + 40 * (1 - t)), int(150 + 50 * (1 - t)), int(170 + 50 * (1 - t)), 255)
            if swirl > 0.55 and d < r - 1.5:
                base = (240, 120 + int(60 * (1 - t)), 40, 255)
            img.putpixel((x, y), base)
    for p in [(5, 4), (4, 5), (5, 5), (6, 4)]:
        img.putpixel(p, (245, 255, 255, 255))
    img.putpixel((10, 11), (200, 240, 245, 255))
    return img


def watch():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 9.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 5.9:
                img.putpixel((x, y), hexc('e2b13c') if d > 4.6 else hexc('f4efe0'))
            if 5.9 < d <= 6.6:
                img.putpixel((x, y), hexc('6e4d12'))
    for p in [(7, 1), (8, 1), (6, 2), (9, 2), (7, 3), (8, 3)]:
        img.putpixel(p, hexc('c99a2e'))
    img.putpixel((7, 2), (0, 0, 0, 0))
    img.putpixel((8, 2), (0, 0, 0, 0))
    for y in range(6, 10):
        img.putpixel((7, y), hexc('2b2b2b'))
    for x in range(5, 8):
        img.putpixel((x, 9), hexc('2b2b2b'))
    for p in [(7, 5), (11, 9), (3, 9), (7, 13)]:
        img.putpixel(p, hexc('8a6a2a'))
    img.putpixel((7, 9), hexc('b8322a'))
    return img


def kite():
    rows = [
        ".......o........",
        "......oRo.......",
        ".....oRRYo......",
        "....oRRRYYo.....",
        "...oRRRRYYYo....",
        "..oRRRRRYYYYo...",
        ".oRRRRRRYYYYYo..",
        "..oBBBBBWWWWo...",
        "...oBBBBWWWo....",
        "....oBBBWWo.....",
        ".....oBBWo......",
        "......oBo.......",
        ".......o........",
        "........s..r....",
        ".......s.ss.....",
        "......b...s.....",
    ]
    return grid(rows, {'o': '3b2414', 'R': 'c8402f', 'Y': 'e8c34a', 'B': '3f6fb5', 'W': 'efe6d2', 's': 'd0d0d0', 'r': 'c8402f', 'b': '3f6fb5'})


def whistle():
    rows = [
        "................",
        "................",
        "................",
        "........oooo....",
        ".......oWWWWo...",
        "......oWWwWWWo..",
        "ooooooWWxxWWWWo.",
        "oMMMMWWWxxWwWWo.",
        "omMMMWWWWWWWWWo.",
        "ooooooWwWWWWWWo.",
        "......oWWWWwWo..",
        ".......oWWWWo.c.",
        "........oooo.c..",
        "............c...",
        "...........c....",
        "................",
    ]
    return grid(rows, {'o': '3d2614', 'W': 'c08f55', 'w': '8f6436', 'x': '140b05', 'M': '9a6a35', 'm': '6b4423', 'c': 'a8a8a8'})


TOYS = {
    'con_quay_linh': (spinning_top, 2),
    'bi_ve_tuan': (marble, 3),
    'dong_ho_mai': (watch, 4),
    'dieu_giay_bao': (kite, 5),
    'coi_go_nam': (whistle, 6),
}

LANG_VI = {
    'item.cavemind.con_quay_linh': 'Con quay của Linh',
    'item.cavemind.con_quay_linh.desc1': 'Con quay gỗ sơn đỏ, vẫn còn ấm.',
    'item.cavemind.con_quay_linh.desc2': 'Linh giấu nó trong hốc cây đếm.',
    'item.cavemind.con_quay_linh.desc3': 'Trời tối, cầm nó chạm vào cây đếm ở nhà Linh.',
    'item.cavemind.bi_ve_tuan': 'Viên bi ve của Tuấn',
    'item.cavemind.bi_ve_tuan.desc1': 'Bên trong có một ngọn lửa nhỏ không bao giờ tắt.',
    'item.cavemind.bi_ve_tuan.desc2': 'Tuấn bảo đây là mặt trời của Tuấn.',
    'item.cavemind.bi_ve_tuan.desc3': 'Tự dựng điểm hẹn rồi cầm nó chạm vào cây đếm.',
    'item.cavemind.dong_ho_mai': 'Đồng hồ đồ chơi của Mai',
    'item.cavemind.dong_ho_mai.desc1': 'Kim đồng hồ chạy ngược.',
    'item.cavemind.dong_ho_mai.desc2': 'Nó luôn chỉ mười một giờ bốn mươi tám.',
    'item.cavemind.dong_ho_mai.desc3': 'Trời tối, cầm nó chạm vào cây đếm ở tháp đồng hồ.',
    'item.cavemind.dieu_giay_bao': 'Con diều của Bảo',
    'item.cavemind.dieu_giay_bao.desc1': 'Dây diều đã đứt từ lâu.',
    'item.cavemind.dieu_giay_bao.desc2': 'Bảo vẫn đợi ở dưới đáy thế giới.',
    'item.cavemind.dieu_giay_bao.desc3': 'Tự dựng điểm hẹn dưới độ cao 0 rồi cầm nó chạm vào cây đếm.',
    'item.cavemind.coi_go_nam': 'Cái còi gỗ của Nam',
    'item.cavemind.coi_go_nam.desc1': 'Thổi vào thì nghe như có người đang đếm.',
    'item.cavemind.coi_go_nam.desc2': 'Ván cuối cùng.',
    'item.cavemind.coi_go_nam.desc3': 'Trời tối, cầm nó chạm vào cây đếm ở trường làng.',
    'cavemind.toy.gate': 'Vật quan trọng: mở chương %s',
    'cavemind.config.chapters': 'Chương nhiệm vụ',
    'cavemind.config.chapters.tooltip': 'Chương 2 tới 6 chỉ mở sau một ván trốn tìm do bạn tự rủ, cầm món đồ chơi của đứa trẻ mất tích ứng với chương đó',
    'cavemind.config.chapters.enabled': 'Bật cổng chương',
    'cavemind.config.chapters.enabled.tooltip': 'Lời mời, điểm hẹn, món đồ chơi, trốn tìm chủ động và quà hiếm',
    'cavemind.config.chapters.spawn_tiers': 'Quái mới theo chương',
    'cavemind.config.chapters.spawn_tiers.tooltip': 'Các loài quái mới chỉ bắt đầu xuất hiện khi có người chơi ở gần đã mở chương mang chúng tới',
    'cavemind.config.chapters.rare_chance': 'Tỉ lệ quà hiếm',
    'cavemind.config.chapters.rare_chance.tooltip': 'Khả năng một ván trốn tìm tự rủ (ngoài cổng chương) kết thúc bằng một vũ khí hiếm thay vì quà nhỏ',
}

LANG_EN = {
    'item.cavemind.con_quay_linh': "Linh's Spinning Top",
    'item.cavemind.con_quay_linh.desc1': 'A red painted wooden top, still warm.',
    'item.cavemind.con_quay_linh.desc2': 'Linh hid it in the counting tree.',
    'item.cavemind.con_quay_linh.desc3': "At night, touch the counting tree at Linh's house with it.",
    'item.cavemind.bi_ve_tuan': "Tuan's Glass Marble",
    'item.cavemind.bi_ve_tuan.desc1': 'A small flame inside that never goes out.',
    'item.cavemind.bi_ve_tuan.desc2': 'Tuan said it was his own sun.',
    'item.cavemind.bi_ve_tuan.desc3': 'Build your own meeting place, then touch the counting tree with it.',
    'item.cavemind.dong_ho_mai': "Mai's Toy Watch",
    'item.cavemind.dong_ho_mai.desc1': 'The hands run backwards.',
    'item.cavemind.dong_ho_mai.desc2': 'It always reads eleven forty eight.',
    'item.cavemind.dong_ho_mai.desc3': 'At night, touch the counting tree at the clock tower with it.',
    'item.cavemind.dieu_giay_bao': "Bao's Kite",
    'item.cavemind.dieu_giay_bao.desc1': 'The string broke long ago.',
    'item.cavemind.dieu_giay_bao.desc2': 'Bao still waits at the bottom of the world.',
    'item.cavemind.dieu_giay_bao.desc3': 'Build your meeting place below height 0, then touch the counting tree with it.',
    'item.cavemind.coi_go_nam': "Nam's Wooden Whistle",
    'item.cavemind.coi_go_nam.desc1': 'Blow it and it sounds like someone counting.',
    'item.cavemind.coi_go_nam.desc2': 'The last game.',
    'item.cavemind.coi_go_nam.desc3': 'At night, touch the counting tree at the village school with it.',
    'cavemind.toy.gate': 'Key item: opens chapter %s',
    'cavemind.config.chapters': 'Quest chapters',
    'cavemind.config.chapters.tooltip': 'Chapters 2 to 6 open only after a hide and seek you start yourself, holding that chapter\'s lost child\'s toy',
    'cavemind.config.chapters.enabled': 'Chapter gates',
    'cavemind.config.chapters.enabled.tooltip': 'Invitations, meeting points, toys, active hide and seek and rare gifts',
    'cavemind.config.chapters.spawn_tiers': 'New monsters per chapter',
    'cavemind.config.chapters.spawn_tiers.tooltip': 'New monster families start spawning only after a nearby player unlocked the chapter that brings them',
    'cavemind.config.chapters.rare_chance': 'Rare gift chance',
    'cavemind.config.chapters.rare_chance.tooltip': 'Chance a hide and seek you start yourself (outside a chapter gate) ends with a rare weapon instead of a small gift',
}

ADV = [
    ('root', None, 'cavemind:con_quay_linh', 'Những Ván Trốn Tìm', 'Mỗi chương mới chỉ mở sau một ván trốn tìm do bạn tự rủ, với món đồ chơi của một đứa trẻ mất tích.', 'task', 'tick'),
    ('tro_tim', 'root', 'minecraft:candle', 'Tự Rủ Nó Chơi', 'Thắng một ván trốn tìm do chính bạn rủ ở một cây đếm', 'task', None),
    ('qua_hiem', 'tro_tim', 'minecraft:diamond_sword', 'Quà Của Đứa Trẻ Thứ Tám', 'Nhận một món vũ khí hiếm sau ván trốn tìm', 'goal', None),
    ('c2_moi', 'root', 'minecraft:compass', 'Lời Mời Của Linh', 'Nhận lời mời của chương 2 và chiếc la bàn sương', 'task', None),
    ('c2_den', 'c2_moi', 'minecraft:hay_block', 'Nhà Của Linh', 'Tìm tới nhà kho bên giếng cạn', 'task', None),
    ('c2_doc', 'c2_den', 'minecraft:written_book', 'Nhật Ký Của Linh', 'Đọc cuốn sổ trên bục đọc', 'task', None),
    ('c2_nen', 'c2_doc', 'minecraft:candle', 'Bốn Ngọn Nến', 'Thắp đủ bốn ngọn nến ở nhà Linh', 'task', None),
    ('c2_mo', 'c2_nen', 'cavemind:con_quay_linh', 'Chương 2: Tiếng Gõ Bên Dưới', 'Thắng ván trốn tìm của Linh', 'challenge', None),
    ('c3_moi', 'c2_mo', 'minecraft:writable_book', 'Lời Mời Của Tuấn', 'Nhận lời mời của chương 3', 'task', None),
    ('c3_dung', 'c3_moi', 'minecraft:bell', 'Sân Chơi Tự Dựng', 'Tự dựng một điểm hẹn đủ luật cho Tuấn', 'task', None),
    ('c3_mo', 'c3_dung', 'cavemind:bi_ve_tuan', 'Chương 3: Nơi Bầu Trời Vỡ Ra', 'Thắng ván trốn tìm của Tuấn', 'challenge', None),
    ('c4_moi', 'c3_mo', 'minecraft:compass', 'Lời Mời Của Mai', 'Nhận lời mời của chương 4 và chiếc la bàn sương', 'task', None),
    ('c4_den', 'c4_moi', 'minecraft:clock', 'Tháp Đồng Hồ Đổ', 'Tìm tới tháp đồng hồ dừng lúc mười một giờ bốn mươi tám', 'task', None),
    ('c4_doc', 'c4_den', 'minecraft:written_book', 'Sổ Tay Của Mai', 'Đọc cuốn sổ ở chân tháp', 'task', None),
    ('c4_nen', 'c4_doc', 'minecraft:candle', 'Năm Ngọn Nến', 'Thắp đủ năm ngọn nến quanh tháp', 'task', None),
    ('c4_mo', 'c4_nen', 'cavemind:dong_ho_mai', 'Chương 4: Sợi Chỉ Của Thời Gian Vỡ Nát', 'Thắng ván trốn tìm của Mai', 'challenge', None),
    ('c5_moi', 'c4_mo', 'minecraft:writable_book', 'Lời Mời Của Bảo', 'Nhận lời mời của chương 5', 'task', None),
    ('c5_dung', 'c5_moi', 'minecraft:deepslate', 'Dưới Đáy Thế Giới', 'Tự dựng một điểm hẹn dưới độ cao 0 cho Bảo', 'task', None),
    ('c5_mo', 'c5_dung', 'cavemind:dieu_giay_bao', 'Chương 5: Hỗn Mang Bung Xích', 'Thắng ván trốn tìm của Bảo', 'challenge', None),
    ('c6_moi', 'c5_mo', 'minecraft:compass', 'Lời Mời Của Nam', 'Nhận lời mời của chương 6 và chiếc la bàn sương', 'task', None),
    ('c6_den', 'c6_moi', 'minecraft:bell', 'Trường Làng', 'Tìm tới ngôi trường trong sương', 'task', None),
    ('c6_doc', 'c6_den', 'minecraft:written_book', 'Sổ Liên Lạc Của Nam', 'Đọc cuốn sổ ở sảnh trường', 'task', None),
    ('c6_nen', 'c6_doc', 'minecraft:candle', 'Sáu Ngọn Nến', 'Thắp đủ sáu ngọn nến trong trường', 'task', None),
    ('c6_mo', 'c6_nen', 'cavemind:coi_go_nam', 'Chương 6: Tận Cùng Của Vạn Vật', 'Thắng ván trốn tìm cuối cùng của Nam', 'challenge', None),
]

RECIPES = {
    'bi_ve_tuan': {
        'conditions': [{'type': 'forge:mod_loaded', 'modid': 'arphex'}, {'type': 'forge:mod_loaded', 'modid': 'born_in_chaos_v1'}],
        'type': 'minecraft:crafting_shaped',
        'pattern': ['NGN', 'GOG', 'NGN'],
        'key': {'N': {'item': 'born_in_chaos_v1:dark_metal_nugget'}, 'G': {'item': 'minecraft:glass'}, 'O': {'item': 'arphex:fire_opal'}},
        'result': {'item': 'cavemind:bi_ve_tuan'},
    },
    'dieu_giay_bao': {
        'conditions': [{'type': 'forge:mod_loaded', 'modid': 'arphex'}, {'type': 'forge:mod_loaded', 'modid': 'born_in_chaos_v1'}],
        'type': 'minecraft:crafting_shaped',
        'pattern': ['PPT', 'PFP', 'SP '],
        'key': {'P': {'item': 'minecraft:paper'}, 'T': {'item': 'arphex:time_prism_shard'}, 'F': {'item': 'born_in_chaos_v1:phantom_powder'}, 'S': {'item': 'minecraft:string'}},
        'result': {'item': 'cavemind:dieu_giay_bao'},
    },
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def merge_lang(name, extra):
    path = os.path.join(ASSETS, 'lang', name)
    d = json.load(open(path, encoding='utf-8'))
    d.update(extra)
    write(path, d)


def main():
    for tid, (fn, gate) in TOYS.items():
        img = fn()
        p = os.path.join(ASSETS, 'textures', 'item', tid + '.png')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img.save(p)
        img.resize((128, 128), Image.NEAREST).save(os.path.join(HERE, '..', 'preview', 'toy_' + tid + '.png'))
        write(os.path.join(ASSETS, 'models', 'item', tid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'cavemind:item/' + tid}})
    for aid, parent, icon, title, desc, frame, trig in ADV:
        a = {}
        if parent:
            a['parent'] = 'cavemind:chuong/' + parent
        a['display'] = {
            'icon': {'item': icon},
            'title': {'text': title},
            'description': {'text': desc},
            'frame': frame,
            'show_toast': parent is not None,
            'announce_to_chat': frame == 'challenge',
            'hidden': False,
        }
        if not parent:
            a['display']['background'] = 'minecraft:textures/block/dark_oak_planks.png'
        a['criteria'] = {'done': {'trigger': 'minecraft:tick' if trig == 'tick' else 'minecraft:impossible'}}
        write(os.path.join(DATA, 'advancements', 'chuong', aid + '.json'), a)
    for rid, r in RECIPES.items():
        write(os.path.join(DATA, 'recipes', rid + '.json'), r)
    merge_lang('vi_vn.json', LANG_VI)
    merge_lang('en_us.json', LANG_EN)
    print('ok', len(TOYS), 'toys', len(ADV), 'advancements', len(RECIPES), 'recipes')


if __name__ == '__main__':
    main()
