package vn.cavemind.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.world.Fx;

public final class Barometer extends Gear.Thing {
    public static final String FOG = "cavemind_fog", NEAR = "cavemind_near";
    private static final Map<UUID, long[]> T = new HashMap<>();

    Barometer() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3);
    }

    public static Vec3 man(MinecraftServer server) {
        Vec3 a = vn.cavemind.man.ManMind.where(server);
        return a != null ? a : vn.cavemind.life.Life.where(server);
    }

    public static double dist(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return Double.MAX_VALUE;
        Vec3 m = man(p.server);
        if (m == null) return Double.MAX_VALUE;
        double d = Math.sqrt(Math.pow(m.x - p.getX(), 2) + Math.pow(m.z - p.getZ(), 2));
        long t = p.server.getTickCount();
        return d * (1 + 0.08 * Mth.sin(t / 97f) * Mth.sin(t / 41f + 1.7f));
    }

    public static int level(double d) {
        return d < 70 ? 2 : d < 200 ? 1 : 0;
    }

    @Override
    public void inventoryTick(ItemStack st, Level lvl, Entity e, int slot, boolean selected) {
        if (lvl.isClientSide || !(e instanceof ServerPlayer p) || slot >= 9 && p.getOffhandItem() != st) return;
        long now = p.server.getTickCount();
        long[] t = T.computeIfAbsent(p.getUUID(), k -> new long[]{-1, 0});
        if (t[0] == now) return;
        t[0] = now;
        double d = dist(p);
        int lv = level(d);
        if (now % 10 == 0) {
            float near = (float) Mth.clamp((230 - d) / 190, 0, 1);
            if (st.getOrCreateTag().getInt(FOG) != lv) st.getOrCreateTag().putInt(FOG, lv);
            if (Math.abs(st.getOrCreateTag().getFloat(NEAR) - near) > 0.01f) st.getOrCreateTag().putFloat(NEAR, near);
        }
        if (lv == 0 || now < t[1]) return;
        var r = p.getRandom();
        int gap = lv == 2 ? 2 + r.nextInt(3) : (int) Mth.lerp(Mth.clamp((d - 70) / 130, 0, 1), 5, 34) + r.nextInt(5);
        t[1] = now + gap;
        Fx.sound(p, "minecraft:block.tripwire.click_on", SoundSource.PLAYERS, p.position(), lv == 2 ? 0.32f : 0.2f, 1.85f + r.nextFloat() * 0.25f);
        if (lv == 2 && r.nextInt(9) == 0) Fx.sound(p, "minecraft:block.powder_snow.step", SoundSource.PLAYERS, p.position(), 0.4f, 1.6f);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack a, ItemStack b, boolean changed) {
        return changed || a.getItem() != b.getItem();
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return vn.cavemind.client.GearRenderer.get();
            }
        });
    }

    public static void forget(UUID id) {
        T.remove(id);
    }
}
