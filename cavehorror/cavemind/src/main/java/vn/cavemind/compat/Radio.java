package vn.cavemind.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import vn.cavemind.sense.Ears;
import vn.cavemind.world.Fx;

public final class Radio {
    static final Set<String> SETS = Set.of("simpleradio:walkie_talkie", "simpleradio:transceiver", "simpleradio:spuddie_talkie");
    private static final Map<UUID, Long> TOLD = new HashMap<>();

    static boolean radio(ItemStack st) {
        if (st.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(st.getItem());
        return id != null && SETS.contains(id.toString());
    }

    public static boolean carries(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (radio(inv.getItem(i))) return true;
        return false;
    }

    public static boolean inHand(ServerPlayer p) {
        return radio(p.getMainHandItem()) || radio(p.getOffhandItem());
    }

    public static void relay(ServerPlayer hider, String line, int beat) {
        if (beat % 4 != 0 && beat != 1 || !carries(hider)) return;
        for (ServerPlayer o : hider.server.getPlayerList().getPlayers()) {
            if (o == hider || o.level() != hider.level() || o.isSpectator() || !carries(o)) continue;
            o.displayClientMessage(Component.literal("[bộ đàm của " + hider.getGameProfile().getName() + "] rè… " + line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            Fx.sound(o, "simpleradio:short_circuit", SoundSource.PLAYERS, o.position(), 0.18f, 1.6f);
        }
    }

    public static void spoke(ServerPlayer speaker, double radius) {
        if (!inHand(speaker)) return;
        for (ServerPlayer o : speaker.server.getPlayerList().getPlayers()) {
            if (o == speaker || o.level() != speaker.level() || !vn.cavemind.damned.Game.active(o) || !carries(o)) continue;
            Ears.noise(o, o.position(), Math.min(12, Math.max(6, radius * 0.6)), Ears.Kind.VOICE);
            Fx.sound(o, "simpleradio:short_circuit", SoundSource.PLAYERS, o.position(), 0.5f, 1.2f);
            long now = o.server.getTickCount();
            if (now - TOLD.getOrDefault(o.getUUID(), -10000L) > 600) {
                TOLD.put(o.getUUID(), now);
                o.displayClientMessage(Component.literal("Bộ đàm trong túi bạn rè lên: " + speaker.getGameProfile().getName() + " đang gọi. Nó nghe thấy rồi.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
        }
    }

    public static void forget(ServerPlayer p) {
        TOLD.remove(p.getUUID());
    }

    private Radio() {}
}
