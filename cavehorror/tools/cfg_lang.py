import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "cavemind"
LANG = ROOT / "src/main/resources/assets/cavemind/lang"

SECTIONS = {
    "chapters": "Chương và trốn tìm chủ động",
    "general": "Chung",
    "director": "Thang căng thẳng",
    "man_from_the_fog": "The Man From The Fog",
    "the_damned": "The Damned",
    "hide_and_seek": "Trò chơi của The Damned",
    "herobrine": "Herobrine",
    "daylight": "Cảnh ban ngày",
}

V = {
    "general.debug": ("Ghi nhật ký quyết định", "Ghi mọi quyết định của các AI vào latest.log (tiền tố [CaveMind])"),
    "general.intensity": ("Mật độ chung", "Mọi thứ xảy ra dày hay thưa. 0,5 là thưa một nửa, 2 là dày gấp đôi"),
    "general.mercy": ("Nương tay", "Không hù khi vừa vào game, đang mở giao diện, đang đánh nhau, cưỡi thú, treo máy, có raid, vừa chết; giới hạn số lần hù mỗi ngày"),
    "general.death_calm_minutes": ("Số phút yên sau khi chết", "Chết xong được yên bao nhiêu phút (chỉ khi bật Nương tay)"),
    "general.pvp_counts": ("Tính cả bạn bè đánh nhau", "Tắt: bạn bè giết nhau, đánh nhau hay dùng /kill không làm AI dừng lại. Bật: được tính như chết thật"),
    "general.glimpses": ("Thoáng thấy", "Sinh vật được phép tự để lộ mình từ xa khi nó muốn: bóng người ngoài cửa sổ, đứng gần hang của nó, ngồi sẵn trong căn nhà tối của bạn"),
    "man_from_the_fog.lives_in_world": ("Sống trong thế giới", "Hắn có hang, có lịch ngày đêm, biết đói sự chú ý, biết dè chừng, biết tò mò. Hắn nghiên cứu căn cứ của bạn qua từng đêm và để lại dấu vết cả khi bạn ngủ"),
    "man_from_the_fog.enters_base": ("Vào trong nhà", "Khi đã quen căn cứ, hắn vào nhà lúc không ai ở nhà (hoặc lúc bạn ngủ) và có thể đứng sẵn trong góc tối khi bạn trở về"),
    "director.enabled": ("Bật thang căng thẳng", "Mỗi người một thang căng thẳng từ 0 tới 100 đi qua bốn pha: Dồn nén, Đỉnh, Nhạt dần, Khoảng lặng. Đạo diễn không bao giờ đặt sinh vật trước mặt bạn, chỉ cho phép, cấm và gợi ý vùng"),
    "director.peak_min": ("Ngưỡng đỉnh (thấp nhất)", "Thang chạm mức này thì vào pha Đỉnh. Mỗi vòng chọn ngẫu nhiên giữa mức thấp nhất và cao nhất"),
    "director.peak_max": ("Ngưỡng đỉnh (cao nhất)", "Mức cao nhất của ngưỡng đỉnh"),
    "director.peak_hold_min": ("Giữ đỉnh (giây, ít nhất)", "Pha Đỉnh kéo dài bao nhiêu giây. Chỉ trong pha Đỉnh sinh vật mới được tấn công hay trừng phạt"),
    "director.peak_hold_max": ("Giữ đỉnh (giây, nhiều nhất)", "Thời gian dài nhất của pha Đỉnh"),
    "director.hint_minutes_min": ("Phút trước khi gợi ý (ít nhất)", "Dồn nén bao lâu mà chưa sinh vật nào phát hiện ra bạn thì đạo diễn gợi ý một vùng cho chúng. Sinh vật vẫn phải tự đi tới và tự tìm"),
    "director.hint_minutes_max": ("Phút trước khi gợi ý (nhiều nhất)", "Mức cao nhất của thời gian chờ gợi ý"),
    "director.dry_chance": ("Vòng khô", "Khả năng một vòng Dồn nén không có gợi ý nào, chỉ có những gì sinh vật tự tìm ra (0 tới 1)"),
    "director.headliner_gate": ("Chặn quái rình chồng chéo", "Không cho quái rình của mod khác chen vào khi đang có cảnh hoặc đang nghỉ"),
    "director.max_headliners": ("Số quái rình cùng lúc", "Bao nhiêu con quái rình được bám một người cùng lúc"),
    "director.headliners": ("Danh sách quái rình", "Mã sinh vật (hoặc modid:*) được tính là quái rình"),
    "director.relax_min": ("Khoảng lặng (phút, ít nhất)", "Sau mỗi đỉnh là một Khoảng lặng thật: không sinh vật nào được lại gần, không phát tiếng. Đây là số phút ít nhất"),
    "director.relax_max": ("Khoảng lặng (phút, nhiều nhất)", "Số phút nhiều nhất của Khoảng lặng"),
    "director.cave_dread": ("Nỗi sợ trong hang", "Tiếng đào sau vách, tiếng bước vọng, tiếng thở trong bóng tối"),
    "director.torch_theft": ("Mất đuốc", "Đuốc bạn cắm xa phía sau có thể biến mất"),
    "director.fog": ("Sương dày lên", "Sương đặc lại khi The Man ở gần"),
    "director.silence": ("Im lặng trước cơn sợ", "Âm thanh và nhạc nền tắt hẳn trước khi có chuyện"),
    "man_from_the_fog.enabled": ("Bật The Man From The Fog", "Thay cách xuất hiện, nhìn, biến mất của The Man bằng bộ não Cave Mind"),
    "man_from_the_fog.frequency": ("Độ háu đói", "Hắn đói nhanh và nghỉ ngắn tới mức nào, nhân thêm với Mật độ chung. 2 là háu gấp đôi"),
    "man_from_the_fog.daytime": ("Đi cả ban ngày", "Khi rất đói hắn có thể đi bộ tới giữa ban ngày trời quang. Dưới hầm thì lúc nào hắn cũng có thể tới"),
    "man_from_the_fog.shy_of_groups": ("Ngại đám đông", "Bật: hắn bỏ dở trò rình khi có người khác trong vòng 22 khối. Tắt: rình cả khi các bạn đi chung"),
    "man_from_the_fog.first_arrival_minutes": ("Phút trước lần đầu", "Người mới chơi bao nhiêu phút thì hắn mới được đi bộ tới gần người đó"),
    "man_from_the_fog.rest_min": ("Nghỉ sau mỗi lần ghé (ít nhất)", "Sau mỗi lần ghé hắn bỏ đi và nghỉ bấy nhiêu phút. Sau đó cơn đói, mùi và tiếng động của bạn sẽ dẫn hắn quay lại"),
    "man_from_the_fog.rest_max": ("Nghỉ sau mỗi lần ghé (nhiều nhất)", "Số phút nghỉ nhiều nhất sau mỗi lần ghé"),
    "man_from_the_fog.hunt": ("Cho phép săn", "Hắn có thể săn bạn (hiếm, có báo trước, trốn được)"),
    "man_from_the_fog.hunt_level": ("Mức quen để săn", "Hắn quen bạn tới mức này (0 tới 10) thì mới săn khi không bị chọc"),
    "man_from_the_fog.hunt_damage": ("Sát thương mỗi đòn", "Sát thương mỗi đòn khi hắn săn"),
    "man_from_the_fog.doors": ("Gõ và mở cửa", "Ban đêm hắn có thể gõ và mở cửa"),
    "man_from_the_fog.schemes": ("Giăng bẫy", "Hắn bày những thứ bạn sẽ tìm thấy sau: lối mòn, ổ của hắn, lỗ nhìn trộm, đường hầm bị bịt"),
    "man_from_the_fog.steal": ("Phá đồ", "Làm thối một phần đồ ăn, đặt lại một hàng cây trồng, bắt một con vật nuôi (không bao giờ con có tên, con đã thuần hay mấy con cuối)"),
    "man_from_the_fog.wake": ("Gõ cho thức dậy", "Gõ cho bạn thức giấc (không hai đêm liền, không khi người khác đang ngủ)"),
    "man_from_the_fog.progress_bar": ("Thanh tiến trình của hắn", "Hiện thanh tiến trình của The Man trên màn hình. Mỗi phần tư thanh kết thúc bằng một việc hắn làm thật: để lại dấu vết ở nhà bạn, tới gần, thử bạn trong bóng tối, rồi một đêm săn không ngủ qua được"),
    "man_from_the_fog.progress_minutes": ("Phút để đầy thanh", "Số phút chơi lúc thức để thanh tự đầy khi không có gì khác. Thấy hắn, ngủ qua đêm và ở dưới hầm làm thanh đầy nhanh hơn"),
    "chapters.enabled": ("Cổng mở chương", "Chương 2 tới 6 chỉ mở sau một ván trốn tìm do bạn tự rủ, cầm món đồ chơi của đứa trẻ ứng với chương đó"),
    "chapters.spawn_tiers": ("Quái theo chương", "Những loài quái mới chỉ bắt đầu xuất hiện khi có người ở gần đã mở chương mang chúng tới"),
    "chapters.rare_chance": ("Khả năng quà hiếm", "Khả năng một ván tự rủ (không phải ván mở chương) kết thúc bằng vũ khí hiếm thay vì quà nhỏ"),
    "the_damned.enabled": ("Bật The Damned", "Thay The Damned bằng một cuộc ám ảnh không giết người"),
    "the_damned.start_day": ("Ngày bắt đầu", "Ngày trong game mà cuộc ám ảnh có thể bắt đầu"),
    "the_damned.night_chance": ("Khả năng mỗi đêm", "Khả năng một đêm có cảnh ám ảnh (0 tới 1)"),
    "the_damned.shrine": ("Điện thờ", "Nó dựng một điện thờ và viết về bạn"),
    "the_damned.sleep_visits": ("Ghé khi ngủ", "Nó tới khi bạn đang ngủ"),
    "the_damned.traces": ("Dấu vết ban đêm", "Nến tắt, khung tranh xoay, đuốc biến mất trong đêm"),
    "the_damned.maze": ("Giờ Thứ Bốn Mươi Tám", "Bị nó bắt thì bị kéo vào mê cung của nó. Đặt một ngọn nến đang cháy lên nắp giếng ở sân chơi rồi rung chuông trường để tự vào chơi. Đồ đạc không bao giờ bị mất ở đó"),
    "the_damned.camcorder": ("Máy quay trong mê cung", "Trong mê cung bạn nhìn qua một chiếc máy quay cũ: nhìn đêm ăn pin, hạt nhiễu, tiếng tim và tiếng thở, không có bản đồ nhỏ"),
    "hide_and_seek.enabled": ("Bật trò chơi của nó", "The Damned tự thấy cô đơn, tự tìm tới bạn, úp mặt vào cây đếm tới bốn mươi tám rồi đi tìm. Chơi đúng luật thì an toàn, phớt lờ nó thì bị phạt"),
    "hide_and_seek.start_day": ("Ngày bắt đầu", "Từ ngày này trong game nó có thể rủ bạn chơi"),
    "hide_and_seek.frequency": ("Độ cô đơn", "Nó thấy cô đơn nhanh tới mức nào. 0,5 là thưa một nửa, 2 là dày gấp đôi"),
    "herobrine.enabled": ("Bật Herobrine", "Herobrine (From The Fog) hiện ra hiếm hoi, đa dạng, như một người chơi thật, nhớ mọi việc bạn làm"),
    "herobrine.replace_from_the_fog": ("Tắt sự kiện gốc của From The Fog", "Tắt các sự kiện gốc (nhìn thấy, đốt nhà, phá đuốc, ác mộng, giả crash) để chỉ Cave Mind điều khiển hắn (cần vào lại world)"),
    "herobrine.frequency": ("Mỏ nhớ nhanh tới mức nào", "Mỗi khối bạn đào hay đặt làm mỏ nhớ bạn thêm một chút, và hắn vọng lại những gì nó nhớ. 2 là gấp đôi"),
    "herobrine.start_day": ("Ngày xuất hiện đầu", "Ngày trong game mà hắn có thể xuất hiện lần đầu"),
    "herobrine.bigger_shows_day": ("Ngày mở cảnh lớn", "Sống sót bao nhiêu ngày thì có kẻ đi bộ, kẻ xây nhà và cảnh lặp lại đường bạn đi"),
    "herobrine.closest_shows_day": ("Ngày mở cảnh sát nhất", "Sống sót bao nhiêu ngày thì có người nằm trên giường bạn và bóng người sau vai"),
    "herobrine.gap_days_min": ("Cách quãng (ít nhất)", "Số ngày trong game giữa hai lần xuất hiện, mức thấp nhất (1 ngày bằng 20 phút)"),
    "herobrine.gap_days_max": ("Cách quãng (nhiều nhất)", "Số ngày trong game giữa hai lần xuất hiện, mức cao nhất"),
    "herobrine.world_edits": ("Để lại dấu vết", "Tấm biển biết thói quen của bạn, đường hầm đào tiếp chỗ bạn bỏ dở, cây trụi lá, bản sao công trình của bạn"),
    "herobrine.fake_leave": ("Tin rời game giả", "Hiếm khi, một dòng báo bạn bè ở xa vừa rời game, chỉ mình bạn thấy (sau khi đã tìm ra mỏ bỏ hoang)"),
    "herobrine.skin": ("Skin", "Đường dẫn skin của Herobrine (bất kỳ skin From The Fog nào trong watching:textures/item/herobrine/...)"),
    "daylight.enabled": ("Bật cảnh ban ngày", "Bóng người giữa đồng, kẻ đi chậm, nghi lễ của thú, ngôi nhà bị động vào, tiếng vọng của chính bạn. Không gây chết, không có gì ném vào màn hình"),
    "daylight.quiet_fallen": ("The Fallen lặng lẽ", "The Fallen lặng lẽ chìm xuống khi bị nhìn, không bật ảnh hù toàn màn hình (cần khởi động lại)"),
}


def title(k):
    return " ".join(w.capitalize() for w in k.split("_"))


def main():
    src = (ROOT / "src/main/java/vn/cavemind/Cfg.java").read_text(encoding="utf-8")
    keys, sec = [], None
    for m in re.finditer(r'push\(b, "(\w+)"|k\(b, "(\w+)"', src):
        if m.group(1):
            sec = m.group(1)
            continue
        keys.append(f"{sec}.{m.group(2)}")
    miss = [k for k in keys if k not in V] + [k for k in V if k not in keys]
    if miss:
        sys.exit(f"mismatch: {miss}")
    for code in ("vi_vn", "en_us"):
        p = LANG / f"{code}.json"
        d = json.loads(p.read_text(encoding="utf-8"))
        d = {k: v for k, v in d.items() if not k.startswith("cavemind.config.")}
        for s, vi in SECTIONS.items():
            d[f"cavemind.config.{s}"] = vi if code == "vi_vn" else title(s)
        for k in keys:
            if code == "vi_vn":
                d[f"cavemind.config.{k}"] = V[k][0]
                d[f"cavemind.config.{k}.tooltip"] = V[k][1]
            else:
                d[f"cavemind.config.{k}"] = title(k.split(".")[1])
        p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(len(keys), "keys")


main()
