package vn.cavemind.damned;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;

public final class Journal {
    public static ItemStack write(ServerPlayer p, Profile pr, boolean banished) {
        ServerLevel lvl = p.serverLevel();
        List<Component> pages = new ArrayList<>();
        String name = p.getGameProfile().getName();
        long day = Dread.day(lvl);
        long first = pr.firstDay >= 0 ? pr.firstDay : 0;
        if (banished) {
            pages.add(Component.translatable("cavemind.journal.banished", name));
        } else {
            pages.add(Component.translatable("cavemind.journal.intro", name, Math.max(1, day - first + 1)));
            pages.add(Component.translatable("cavemind.journal.turns", pr.turnArounds, Math.max(1, pr.turnsWhileWatched + pr.manSightings / 2)));
            Set<String> used = new HashSet<>();
            int count = 0;
            for (int i = pr.moments.size() - 1; i >= 0 && count < 5; i--) {
                Moment m = pr.moments.get(i);
                if (used.contains(m.kind())) continue;
                Component c = moment(m);
                if (c == null) continue;
                used.add(m.kind());
                pages.add(c);
                count++;
            }
            int deaths = p.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
            int sleeps = p.getStats().getValue(Stats.CUSTOM.get(Stats.SLEEP_IN_BED));
            int hours = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 72000;
            pages.add(Component.translatable("cavemind.journal.stats", deaths, sleeps, Math.max(1, hours)));
            if (pr.dStolen > 0) pages.add(Component.translatable("cavemind.journal.stolen", pr.dStolen));
            if (pr.dHurt > 0) pages.add(Component.translatable("cavemind.journal.hurt_count", pr.dHurt));
            pages.add(Component.translatable(pr.dStage >= 4 ? "cavemind.journal.end4" : "cavemind.journal.end"));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", name);
        tag.putString("author", "§8§k......");
        tag.putInt("generation", 3);
        ListTag list = new ListTag();
        for (Component c : pages) list.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        tag.put("pages", list);
        tag.putBoolean("resolved", true);
        tag.putBoolean("cavemind", true);
        pr.dJournals++;
        return book;
    }

    static Component moment(Moment m) {
        String t = clock(m.time());
        long d = m.day() + 1;
        int x = m.pos().getX(), y = m.pos().getY(), z = m.pos().getZ();
        return switch (m.kind()) {
            case Moment.SEEN -> Component.translatable("cavemind.journal.m.seen", d, t, m.a());
            case Moment.UNSEEN -> Component.translatable("cavemind.journal.m.unseen", d, t, x, y, z);
            case Moment.TURNED -> Component.translatable("cavemind.journal.m.turned", d, t);
            case Moment.LISTENED -> Component.translatable("cavemind.journal.m.listened", d, t, m.a());
            case Moment.TOOK -> Component.translatable("cavemind.journal.m.took", d, x, y, z);
            case Moment.DIED -> Component.translatable("cavemind.journal.m.died", d, t, x, y, z);
            case Moment.SLEPT -> Component.translatable("cavemind.journal.m.slept", d, t,
                Component.translatable("cavemind.dir." + Direction.from2DDataValue(m.a()).getName()));
            case Moment.ESCAPED -> Component.translatable("cavemind.journal.m.escaped", d);
            case Moment.HURT_IT -> Component.translatable("cavemind.journal.m.hurt", d);
            case Moment.FOLLOWED -> Component.translatable("cavemind.journal.m.followed", d, t);
            case Moment.HOME -> Component.translatable(m.b() == 1 ? "cavemind.journal.m.home_seen" : "cavemind.journal.m.home", d, t);
            case Moment.NOON -> Component.translatable("cavemind.journal.m.noon", d, m.a());
            case Moment.ECHOED -> Component.translatable("cavemind.journal.m.echoed", d, x, y, z);
            case Moment.RITUAL -> Component.translatable(m.b() == 1 ? "cavemind.journal.m.village" : "cavemind.journal.m.ritual", d, m.a());
            case Moment.TWIN -> Component.translatable("cavemind.journal.m.twin", d);
            case Moment.SEEK -> Component.translatable(m.b() == 1 ? "cavemind.journal.m.seek_back" : "cavemind.journal.m.seek", d, t, m.a());
            case Moment.TRAIL -> Component.translatable("cavemind.journal.m.trail", d);
            case Moment.PRINTS -> Component.translatable("cavemind.journal.m.prints", d);
            case Moment.TORCHES -> Component.translatable("cavemind.journal.m.torches", d, x, y, z);
            case Moment.INSIDE -> Component.translatable("cavemind.journal.m.inside", d);
            case Moment.DOOR -> Component.translatable("cavemind.journal.m.door", d);
            case Moment.SITIN -> Component.translatable("cavemind.journal.m.sitin", d, t);
            case Moment.WINDOW -> Component.translatable("cavemind.journal.m.window", d, t);
            case Moment.NEST -> Component.translatable("cavemind.journal.m.nest", d);
            case Moment.SPOIL -> Component.translatable("cavemind.journal.m.spoil", d);
            case Moment.HARVEST -> Component.translatable("cavemind.journal.m.harvest", d);
            case Moment.PEEP -> Component.translatable("cavemind.journal.m.peep", d);
            case Moment.SEAL -> Component.translatable("cavemind.journal.m.seal", d, t, x, y, z);
            case Moment.WAKE -> Component.translatable("cavemind.journal.m.wake", d, t);
            case Moment.ANIMAL -> Component.translatable("cavemind.journal.m.animal", d);
            case Moment.LURED -> Component.translatable("cavemind.journal.m.lured", d, t);
            case Moment.FRENZY -> Component.translatable("cavemind.journal.m.frenzy", d, t, m.a(), m.b());
            case Moment.HERO -> Component.translatable("cavemind.journal.m.hero" + Math.min(6, Math.max(0, m.b())), d, t, m.a());
            case Moment.HERO_SIGN -> Component.translatable("cavemind.journal.m.hero_sign", d, x, y, z);
            case Moment.HERO_TUNNEL -> Component.translatable("cavemind.journal.m.hero_tunnel", d);
            case Moment.HERO_BUILD -> Component.translatable("cavemind.journal.m.hero_build", d);
            case Moment.HERO_LEAFLESS -> Component.translatable("cavemind.journal.m.hero_leafless", d);
            case Moment.BELL -> Component.translatable(m.b() == 1 ? "cavemind.journal.m.bell" : "cavemind.journal.m.bell_failed", d, t);
            case Moment.HID -> Component.translatable("cavemind.journal.m.hid", d, t);
            case Moment.LORE -> Component.translatable("cavemind.journal.m.lore" + m.a(), d, x, y, z);
            default -> null;
        };
    }

    static String clock(int tod) {
        int h = (tod / 1000 + 6) % 24;
        int min = (tod % 1000) * 60 / 1000;
        return String.format("%02d:%02d", h, min);
    }

    private Journal() {}
}
