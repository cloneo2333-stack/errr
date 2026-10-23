package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.gear.Barometer;
import vn.cavemind.gear.Gear;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class GearSounds {
    static final RandomSource R = RandomSource.create();
    static Item held;
    static float lastNeedle;
    static int tick, nextFly, nextHum;

    static void play(String id, float vol, float pitch) {
        SoundEvent s = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(id));
        if (s != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    static ItemStack find(Minecraft mc, Item it) {
        if (mc.player.getMainHandItem().is(it)) return mc.player.getMainHandItem();
        if (mc.player.getOffhandItem().is(it)) return mc.player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        tick++;
        Item now = mc.player.getMainHandItem().getItem();
        if (now != held) {
            held = now;
            if (now == Gear.FINDER.get()) play("minecraft:block.glass.hit", 0.35f, 1.7f);
            else if (now == Gear.BARO.get()) play("minecraft:item.armor.equip_chain", 0.4f, 1.6f);
            else if (now == Gear.LISTEN.get()) play("minecraft:block.lever.click", 0.35f, 1.8f);
        }
        ItemStack baro = find(mc, Gear.BARO.get());
        if (!baro.isEmpty()) {
            float n = GearRenderer.needle;
            if (Math.abs(n - lastNeedle) > 6f) {
                play("minecraft:block.chain.step", 0.18f, 1.9f + R.nextFloat() * 0.2f);
                lastNeedle = n;
            }
            int lv = baro.hasTag() ? baro.getTag().getInt(Barometer.FOG) : 0;
            if (lv == 2 && tick % 23 == 0) play("minecraft:block.glass.step", 0.12f, 1.9f);
        }
        ItemStack jar = find(mc, Gear.FINDER.get());
        if (!jar.isEmpty() && tick >= nextFly) {
            boolean full = !mc.player.getCooldowns().isOnCooldown(Gear.FINDER.get());
            if (full) play("minecraft:block.beehive.work", 0.12f, 1.9f + R.nextFloat() * 0.15f);
            nextFly = tick + 50 + R.nextInt(70);
        }
        ItemStack ls = find(mc, Gear.LISTEN.get());
        if (!ls.isEmpty()) {
            if (tick % 20 == 0) play("minecraft:ui.button.click", 0.05f, 2.0f);
            if (tick >= nextHum) {
                play("minecraft:block.beacon.ambient", 0.08f, 1.9f);
                nextHum = tick + 60;
            }
            if (SeekFx.scanTicks > 0 && SeekFx.scan > 0.35f && tick % 6 == 0) play("minecraft:block.note_block.bit", 0.25f, 1.4f + SeekFx.scan * 0.6f);
            else if (SeekFx.scanTicks > 0 && tick % 14 == 0) play("minecraft:block.note_block.bit", 0.1f, 0.9f);
        }
    }

    private GearSounds() {}
}
