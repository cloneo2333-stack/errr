package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class ClientTest {
    static final boolean ON = Boolean.getBoolean("cavemind.clienttest");
    static final String MODE = System.getenv().getOrDefault("CAVEMIND_TEST", "all");
    static int t, m, modeFrames;
    static boolean guideShot;

    static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "cm_" + name + ".png", mc.getMainRenderTarget(), m -> CaveMind.LOG.info("[CLIENTTEST] " + m.getString()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.RenderTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen instanceof ModeScreen && t < 60) {
            if (++modeFrames == 90) {
                shot(mc, "mode");
                mc.setScreen(null);
            }
            return;
        }
        if (mc.screen != null && t < 60) return;
        t++;
        if (t == 59) {
            t = switch (MODE) {
                case "book" -> 239;
                case "seek" -> 349;
                case "maze" -> 479;
                default -> 59;
            };
            CaveMind.LOG.info("[CLIENTTEST] scenario " + MODE);
        }
        if (!MODE.equals("all") && (MODE.equals("book") && t == 356 || MODE.equals("seek") && t == 446)) mc.stop();
        if (t == 60) ManBar.on(new Net.Bar(62, 2, false, "Hắn đã biết chỗ bạn ngủ"));
        if (MODE.equals("items")) {
            items(mc);
            return;
        }
        if (t == 140) shot(mc, "bar");
        if (t == 150) ManBar.on(new Net.Bar(100, 3, true, "ĐÊM SĂN: không thể ngủ. Giữ ánh sáng, rung chuông, sống sót"));
        if (t == 230) shot(mc, "bar_due");
        if (t == 240) BookClient.ask(Net.Ask.OPEN, 0);
        if (t == 290) shot(mc, "book_guide");
        if (t == 292 && mc.screen instanceof BookScreen bs) bs.go(4);
        if (t == 305) shot(mc, "book_guide4");
        if (t == 307 && mc.screen instanceof BookScreen bs) {
            bs.go(8);
            bs.flipped = true;
        }
        if (t == 318) shot(mc, "book_guide8");
        if (t == 320 && mc.screen instanceof BookScreen bs) bs.tab = 2;
        if (t == 330) shot(mc, "book_chapters");
        if (t == 332 && mc.screen instanceof BookScreen bs) {
            bs.tab = 5;
            bs.gear = 4;
        }
        if (t == 336) shot(mc, "book_gear");
        if (t == 337 && mc.screen instanceof BookScreen bs) bs.gear = 7;
        if (t == 339) shot(mc, "book_moon");
        if (t == 341 && mc.screen instanceof BookScreen bs) bs.tab = 3;
        if (t == 343) shot(mc, "book_rules");
        if (t == 344 && mc.screen instanceof BookScreen bs) bs.tab = 1;
        if (t == 348) shot(mc, "book_goals");
        if (t == 352) mc.setScreen(null);
        if (t == 350) ClientFx.on(Net.DARK, 1f, 20);
        if (t == 390) shot(mc, "seek_dark");
        if (t == 392) ClientFx.on(Net.HOLD, 1f, 120);
        if (t == 430) shot(mc, "seek_hold");
        if (t == 440) {
            ClientFx.on(Net.HOLD, -1f, 0);
            ClientFx.on(Net.DARK, 0f, 10);
        }
        if (t == 480) mc.player.connection.sendCommand("cmmaze");
        if (!ClientFx.maze()) {
            if (t > 2400) mc.stop();
            return;
        }
        m++;
        if (m > 40 && m < 600 && mc.screen != null) {
            if (m < 200 && mc.screen instanceof BookScreen && !guideShot) {
                guideShot = true;
                shot(mc, "maze_guide");
            }
            mc.setScreen(null);
        }
        if (m < 240) MazeHud.on(new Net.Cam(true, 0.6f, false, 0.0f, 2, 400, false, 1f, 0.3f, false, false, true, 30f, 0));
        else if (m < 360) MazeHud.on(new Net.Cam(true, 0.55f, true, 0.1f, 2, 390, false, 0.8f, 0.2f, false, false, true, 12f, 0));
        else if (m < 480) MazeHud.on(new Net.Cam(true, 0.5f, false, 0.92f, 3, 380, true, 0.45f, 0.9f, false, true, true, 3f, 1));
        else MazeHud.on(new Net.Cam(true, 0.07f, true, 0.85f, 3, 370, false, 0.1f, 0.8f, true, false, true, 5f, 0));
        boolean nvOn = m >= 240 && m < 360 || m >= 480;
        if (nvOn && !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2000, 0, false, false));
        if (!nvOn && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) mc.player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        mc.options.keyUp.setDown(m > 100 && m < 220 || m > 380 && m < 470);
        if (m > 100 && m < 220) mc.player.setYRot(mc.player.getYRot() + 0.6f);
        if (MODE.equals("maze") && m > 240 && m < 360 && m % 3 == 0) film(mc, "maze");
        if (m == 230) shot(mc, "dark");
        if (m == 350) shot(mc, "nv");
        if (m == 470) shot(mc, "danger");
        if (m == 590) shot(mc, "nv_danger");
        if (m == 620) {
            CaveMind.LOG.info("[CLIENTTEST] maze steps " + MazeMove.steps + " muted vanilla " + MazeMove.muted + " jump " + mc.options.keyJump.isDown());
            mc.stop();
        }
    }

    static int frame;

    static void film(Minecraft mc, String clip) {
        shot(mc, "f_" + clip + "_" + String.format("%03d", frame++));
    }

    static void hold(Minecraft mc, net.minecraft.world.item.ItemStack st) {
        select(mc, 8);
        mc.player.getInventory().setItem(8, st);
    }

    static void select(Minecraft mc, int i) {
        mc.player.getInventory().selected = i;
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(i));
    }

    static void items(Minecraft mc) {
        if (t == 61) {
            mc.player.setXRot(8f);
            hold(mc, new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.BARO.get()));
        }
        if (t >= 90 && t < 280 && mc.player.getInventory().selected != 8 && !mc.player.getMainHandItem().is(vn.cavemind.gear.Gear.BARO.get())) hold(mc, new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.BARO.get()));
        if (t >= 100 && t < 280) {
            float near = Math.min(1f, (t - 100) / 150f);
            var st = mc.player.getMainHandItem();
            if (st.is(vn.cavemind.gear.Gear.BARO.get())) {
                st.getOrCreateTag().putFloat(vn.cavemind.gear.Barometer.NEAR, near);
                st.getOrCreateTag().putInt(vn.cavemind.gear.Barometer.FOG, near > 0.82f ? 2 : near > 0.25f ? 1 : 0);
            }
            if (t % 3 == 0) film(mc, "baro");
        }
        if (t == 282) {
            frame = 0;
            hold(mc, net.minecraft.world.item.ItemStack.EMPTY);
            for (int i = 0; i < 8; i++) if (mc.player.getInventory().getItem(i).is(vn.cavemind.gear.Gear.FINDER.get())) {
                select(mc, i);
                break;
            }
        }
        if (t >= 320 && t < 340 && t % 3 == 0) film(mc, "jar");
        if (t == 340) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (t > 340 && t < 520 && t % 3 == 0) film(mc, "jar");
        if (t == 522) {
            frame = 0;
            hold(mc, new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.LISTEN.get()));
        }
        if (t >= 560 && t < 600 && t % 3 == 0) film(mc, "listen");
        if (t > 600 && t < 720) {
            SeekFx.scan(t > 660 ? 0.85f : 0.15f, 10);
            if (t % 3 == 0) film(mc, "listen");
        }
        if (t == 720) {
            mc.stop();
        }
    }

    private ClientTest() {}
}
