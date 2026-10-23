import html, json, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
P = os.path.join(ROOT, 'preview')
LANG = json.load(open(os.path.join(ROOT, 'cavemind', 'src', 'main', 'resources', 'assets', 'cavemind', 'lang', 'vi_vn.json'), encoding='utf-8'))
IDX = json.load(open(os.path.join(P, 'lore', 'index.json'), encoding='utf-8'))
L = lambda k: LANG['cavemind.lore.' + k]

SITES = [
    ('s1', 'Ngày 1 · Trạm đo sương số một', 'The Man From The Fog',
     'Căn chòi gỗ vân sam, cửa mở toang. Thùng bánh mốc đúng một nửa, vạch đếm ngày trên tường, chuông báo sương trên cột ngoài hiên. Lối mòn mới bắt đầu hình thành từ phía tây.',
     [('Sổ trên bục giảng', 's1', 5)]),
    ('g', 'Ngày 2 · Bãi đếm', 'The Damned',
     'Cái cây chết đóng biển luật chơi, vòng nến tắt quanh gốc. Hình nộm đứa trẻ áo đỏ úp mặt vào gốc bạch dương, hai tay che mắt. Tấm biển đếm kẹt ở bốn mươi tám. Sách luật nằm trong rương dưới gốc cây.',
     [('Sách trong rương', 'g', 6)]),
    ('s2', 'Ngày 3 · Trạm đo sương số hai', 'The Man From The Fog',
     'Lối mòn giờ chạy thẳng từ rừng tới đúng cửa sổ. Cửa sổ bị đóng ván, tường cạnh đầu giường thủng một lỗ đục từ ngoài vào. Thùng chỉ còn thịt thối.',
     [('Sổ trên bục giảng', 's2', 5)]),
    ('h', 'Ngày 4 · Nhà của lũ trẻ', 'The Damned',
     'Ba chiếc giường nhỏ. Hai tấm biển ghi bảy cái tên bị gạch, dòng cuối là tên của người chơi tìm ra ngôi nhà, không bị gạch. Tủ khóa có đầu lâu đặt trên rương. Một ngọn nến còn cháy trước bậc cửa.',
     [('Sách trong tủ khóa', 'h', 4)]),
    ('s3', 'Ngày 6 · Trạm đo sương cuối cùng', 'The Man From The Fog',
     'Cửa đã mất. Giường bị đem ra ngoài đồng, gối quay về phía rừng. Chuông rơi dưới chân cột, đèn lồng giờ là lửa linh hồn. Sổ dừng ở chữ "Ngày 2".',
     [('Sổ trên bục giảng', 's3', 5)]),
]


def book(key, n):
    pages = ''.join('<div class="page"><span class="pn">%d</span>%s</div>' % (i, html.escape(L('%s.p%d' % (key, i))).replace('\n', '<br>')) for i in range(1, n + 1))
    return '<details><summary>%s</summary><div class="pages">%s</div></details>' % (html.escape(L(key + '.title')), pages)


def main():
    cards = []
    for k, title, who, desc, books in SITES:
        imgs = ''.join('<figure><img src="lore/%s" loading="lazy" alt=""><figcaption>%s</figcaption></figure>' % (fn, html.escape(cap)) for cap, fn in IDX[k])
        bk = ''.join('<div class="bk"><h4>%s</h4>%s</div>' % (html.escape(t), book(key, n)) for t, key, n in books)
        cards.append('<section><header><h2>%s</h2><span class="who">%s</span></header><p>%s</p><div class="grid">%s</div>%s</section>' % (
            html.escape(title), html.escape(who), html.escape(desc), imgs, bk))
    page = TPL.replace('%CARDS%', '\n'.join(cards))
    open(os.path.join(P, 'index.html'), 'w', encoding='utf-8').write(page)
    print('ok')


TPL = """<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Xem trước Cave Mind 1.7</title>
<style>
:root{--bg:#0d0f14;--card:#151922;--line:#262c39;--text:#dde1ea;--dim:#8f98aa;--accent:#c9534a}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.6 system-ui,"Segoe UI",sans-serif}
.wrap{max-width:1100px;margin:0 auto;padding:28px 16px 60px}h1{font-size:24px;margin:0 0 6px}.lead{color:var(--dim);margin:0 0 22px}
a.btn{display:inline-block;background:#2a1e20;border:1px solid var(--accent);color:var(--text);text-decoration:none;padding:9px 14px;border-radius:9px;margin:0 0 26px}
section{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:18px 18px 10px;margin:0 0 22px}
header{display:flex;justify-content:space-between;align-items:baseline;gap:10px;flex-wrap:wrap}h2{font-size:18px;margin:0}
.who{color:var(--accent);font-size:13px}section p{color:var(--dim);margin:6px 0 14px}
.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}figure{margin:0}img{width:100%;border-radius:8px;display:block;border:1px solid var(--line)}
figcaption{font-size:12.5px;color:var(--dim);padding:4px 2px 0}.bk h4{margin:14px 0 6px;font-size:14px}
details{border:1px solid var(--line);border-radius:9px;padding:8px 12px;margin-bottom:10px}summary{cursor:pointer;font-weight:600}
.pages{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:10px;margin-top:10px}
.page{background:#e9dfc7;color:#2a2219;border-radius:4px;padding:12px 12px 14px;font:14px/1.5 "Times New Roman",Cambria,"Noto Serif",serif;position:relative;min-height:150px}
.pn{position:absolute;right:8px;bottom:4px;font-size:11px;color:#7d6c55}
@media(max-width:640px){.grid{grid-template-columns:1fr}}
</style></head><body><div class="wrap">
<h1>Cave Mind 1.7, xem trước ngoại tuyến</h1>
<p class="lead">Ảnh dựng bằng mô hình khối thật của Minecraft, theo đúng tọa độ trong mod. Giường, rương, biển, chuông, đầu lâu và hình nộm được vẽ thay thế vì game dùng bộ vẽ riêng cho chúng. Trong game địa điểm sẽ nằm trên địa hình thật, cách bạn 80 tới 130 khối theo hướng bạn đang đi.</p>
<a class="btn" href="damned.html">Mở trình xem 3D The Damned (động tác mới)</a>
%CARDS%
</div></body></html>"""

main()
