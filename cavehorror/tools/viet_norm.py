import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import viet
R = [
 (r'đá phiến sâu', 'đá bảng sâu'), (r'Đá phiến sâu', 'Đá bảng sâu'),
 (r'\bđá đỏ\b', 'Redstone'), (r'\bĐá đỏ\b', 'Redstone'),
 (r'\b[Đđ]á Nether\b', 'Netherrack'),
 (r'lăng chuẩn', 'Prismarin'), (r'Lăng chuẩn', 'Prismarin'),
 (r'\b[đĐ]á tuf\b', lambda m: m.group(0)[0] + 'á túp'),
 (r'\bgranit\b', 'hoa cương'),
 (r'sa thạch', 'cát kết'), (r'Sa thạch', 'Cát kết'),
 (r'[Mm]ụn cóc Nether', 'Bướu Nether'), (r'mụn cóc', 'bướu'), (r'Mụn cóc', 'Bướu'),
 (r'(?<!ngọc )(?<!Ngọc )\blưu ly\b', 'ngọc lưu ly'), (r'\bLưu ly\b', 'Ngọc lưu ly'),
 (r'[Kk]ỳ giông', 'Axolotl'),
 (r'mảnh vụn Netherit', 'vụn Netherit'), (r'Mảnh vụn Netherit', 'Vụn Netherit'),
 (r'Giá sách chạm khắc', 'Kệ sách được đục'), (r'giá sách chạm khắc', 'kệ sách được đục'),
 (r'Thế giới chính', 'Thế giới thực'), (r'thế giới chính', 'Thế giới thực'),
 (r'xanh dương nhạt', 'xanh nhạt'), (r'xanh dương', 'xanh nước biển'),
 (r'gỗ keo', 'gỗ xiêm gai'), (r'gỗ rừng rậm', 'gỗ rừng'), (r'gỗ sồi sẫm màu', 'gỗ sồi sẫm'), (r'gỗ biến dị', 'gỗ kì dị'),
]
tot = {}
for f in sorted(os.listdir(viet.BATCH)):
    if not f.endswith('.vi.txt'): continue
    p = os.path.join(viet.BATCH, f); s = open(p, encoding='utf-8').read(); o = s
    for a, b in R:
        s, n = re.subn(a, b, s)
        if n: tot[a] = tot.get(a, 0) + n
    s = re.sub(r'(\t)(\w)', lambda m: m.group(1) + m.group(2).upper(), s)
    if s != o: open(p, 'w', encoding='utf-8').write(s)
print(tot)
