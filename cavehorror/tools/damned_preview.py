import base64, json, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
GEO = os.path.join(ROOT, 'decomp', 'apollyon', 'assets', 'apollyon', 'geo', 'thedamned.geo.json')
ANIM = os.path.join(ROOT, 'cavemind', 'src', 'main', 'resources', 'overrides', 'assets', 'apollyon', 'animations', 'thedamned.animation.json')
TEX = os.path.join(ROOT, 'decomp', 'apollyon', 'assets', 'apollyon', 'textures', 'entities')
OUT = os.path.join(ROOT, 'preview', 'damned.html')

SHOW = [
    ('cm_point', 'Chỉ tay (mới)', 'Cuối trò trốn tìm, khi nó đứng ở chỗ bạn bắt đầu chơi và bạn nhìn nó đủ lâu.'),
    ('cm_owl', 'Quay đầu cú mèo', 'Cuối trò trốn tìm, khi nó đứng ngay sau lưng bạn, quay lưng lại, rồi đầu xoay ngược ra nhìn bạn.'),
    ('animation.theDamned.spotted', 'Nhận ra bạn (gốc)', 'Beckon, Indoors: đang đứng nghiêng thì xoay đầu nhìn thẳng vào bạn.'),
    ('animation.theDamned.found', 'Tìm thấy bạn (gốc)', 'Lúc bạn ngủ, nó đứng ở đầu giường.'),
    ('cm_notice', 'Nhận ra bạn (mới)', 'Mirror: cái bóng đang bắt chước bạn bỗng dừng lại.'),
    ('cm_profile', 'Đứng nghiêng', 'Tư thế chờ trước khi nó xoay đầu nhìn bạn.'),
    ('animation.theDamned.idle', 'Đứng thẳng (đã sửa)', 'Tư thế mặc định, cũng là tư thế khi nó di chuyển. Trước đây nó bị nghiêng 90 độ.'),
    ('cm_still', 'Đứng im', 'Trốn tìm: nó đã thấy bạn, đứng nhìn thẳng.'),
    ('cm_drift_a', 'Trôi', 'Mirror và Beckon: dáng đi trôi dạt.'),
    ('cm_count', 'Úp mặt đếm', 'Đầu trò trốn tìm, mặt úp vào thân cây.'),
    ('cm_search_a', 'Đi tìm', 'Trốn tìm: ngó nghiêng tìm bạn.'),
    ('cm_peek', 'Ló ra sau cây', 'Trốn tìm: nghiêng người ló ra từ sau thân cây.'),
    ('cm_low', 'Rạp thấp', 'Khi bạn sáp lại gần, nó rạp xuống rồi lao vụt đi.'),
    ('cm_tall', 'Vươn cao', 'Trốn tìm, giai đoạn đã thấy bạn.'),
    ('cm_lean', 'Chồm mặt', 'Khi bạn tới gần, nó chồm mặt về phía bạn.'),
    ('animation.theDamned.chase', 'Rượt (gốc, tham chiếu)', 'Động tác gốc của mod, dùng để đối chiếu hướng mặt.'),
]


def b64(p):
    return base64.b64encode(open(p, 'rb').read()).decode()


def main():
    geo = json.load(open(GEO, encoding='utf-8'))['minecraft:geometry'][0]
    anims = json.load(open(ANIM, encoding='utf-8'))['animations']
    data = {
        'tw': geo['description']['texture_width'], 'th': geo['description']['texture_height'],
        'bones': geo['bones'],
        'anims': {k: anims[k] for k, _, _ in SHOW},
        'list': [[k, n, d] for k, n, d in SHOW],
    }
    html = open(os.path.join(ROOT, 'tools', 'damned_preview.tpl.html'), encoding='utf-8').read()
    html = html.replace('/*DATA*/null', json.dumps(data, ensure_ascii=False, separators=(',', ':')))
    html = html.replace('%BASE%', b64(os.path.join(TEX, 'apollyonbasenoise.png')))
    html = html.replace('%GLOW%', b64(os.path.join(TEX, 'apollyonemissive.png')))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, 'w', encoding='utf-8').write(html)
    print(OUT, len(html) // 1024, 'KB')


main()
