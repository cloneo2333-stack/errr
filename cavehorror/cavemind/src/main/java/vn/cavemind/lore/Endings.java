package vn.cavemind.lore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import vn.cavemind.CaveMind;
import vn.cavemind.compat.Compat;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.world.Fx;

public final class Endings {
    public static void check(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        if (!pr.dPeace || !pr.manPeace || !pr.heroPeace || pr.endingSeen) return;
        pr.endingSeen = true;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(30, 120, 40));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Thung lũng thở ra. Lần này, nó không hít vào nữa.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Bản Sương").withStyle(ChatFormatting.WHITE)));
        Fx.sound(p, "minecraft:block.bell.resonate", SoundSource.AMBIENT, p.position(), 0.7f, 0.6f);
        Compat.grant(p, "cavemind:ban_suong/ban_suong");
        CaveMind.debug("{} saw the end of Ban Suong", p.getGameProfile().getName());
    }

    public static String describe(ServerPlayer p) {
        Profile pr = MindData.profile(p);
        return "đứa trẻ " + (pr.dPeace ? "đã về nhà" : "chưa") + ", thợ săn " + (pr.manPeace ? "đã về nhà" : "chưa") + ", bản sao " + (pr.heroPeace ? "đã chớp mắt" : "chưa");
    }

    private Endings() {}
}
