import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import viet
R = viet.ROOT
w = json.load(open(os.path.join(R, 'work.json'), encoding='utf-8'))
items = w['items']
C = {'White':'trắng','Orange':'cam','Magenta':'đỏ sậm','Light Blue':'xanh nhạt','Yellow':'vàng','Lime':'xanh lá mạ','Pink':'hồng','Gray':'xám','Light Gray':'xám nhạt','Cyan':'lục lam','Purple':'tím','Blue':'xanh nước biển','Brown':'nâu','Green':'xanh lá cây','Red':'đỏ','Black':'đen'}
W = {'Oak':'gỗ sồi','Spruce':'gỗ vân sam','Birch':'gỗ bạch dương','Jungle':'gỗ rừng','Acacia':'gỗ xiêm gai','Dark Oak':'gỗ sồi sẫm','Mangrove':'gỗ đước','Cherry':'gỗ anh đào','Bamboo':'gỗ tre','Crimson':'gỗ đỏ thẫm','Warped':'gỗ kì dị'}
O = {'Basin':'Chậu rửa','Bath':'Bồn tắm','Kitchen Cabinetry':'Tủ bếp','Kitchen Drawer':'Ngăn kéo bếp','Kitchen Sink':'Bồn rửa bếp','Kitchen Storage Cabinet':'Tủ chứa đồ bếp','Toilet':'Bồn cầu','Candle Coffee Cake':'Bánh bông lan cắm nến','Canvas Wall Sign':'Biển vải treo tường','Cooler':'Thùng giữ lạnh','Cushion':'Đệm','Cushion Slab':'Phiến đệm','Cushion Stairs':'Bậc đệm','Glazed Urn':'Bình gốm tráng men','Grill':'Lò nướng','Lamp':'Đèn','Sofa':'Ghế sô pha','Stool':'Ghế đẩu','Trampoline':'Bạt nhún','Wall Hanging Canvas Sign':'Biển vải treo','Wool Awning':'Mái hiên len','Carved Pumpkin':'Bí ngô được khắc','Jack o\'Lantern':'Đèn bí ngô','Banister':'Lan can','Coffin':'Quan tài','Large Shelf':'Kệ lớn','Chair':'Ghế','Crate':'Thùng gỗ','Cutting Board':'Thớt','Dark Ceiling Fan':'Quạt trần tối','Desk':'Bàn làm việc','Drawer':'Tủ ngăn kéo','Lattice Fence':'Hàng rào mắt cáo','Lattice Fence Gate':'Cổng rào mắt cáo','Light Ceiling Fan':'Quạt trần sáng','Mailbox':'Hộp thư','Storage Cabinet':'Tủ chứa đồ','Storage Jar':'Hũ chứa đồ','Table':'Bàn','Hedge':'Hàng rào cây'}
done = viet.read_vi()
out = []
for i, s in enumerate(items):
    if i in done: continue
    for p in sorted(list(C) + list(W), key=len, reverse=True):
        if s.startswith(p + ' ') and s[len(p)+1:] in O:
            mod = C.get(p) or W[p]
            out.append('%d\t%s %s' % (i, O[s[len(p)+1:]], mod))
            break
open(os.path.join(R, 'batches', 'auto_compose.vi.txt'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(out))
