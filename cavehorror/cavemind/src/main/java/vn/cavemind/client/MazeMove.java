package vn.cavemind.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class MazeMove {
    static float fwd, side, bob, bobAmp, lastBob;
    static double stride, lx, lz;
    static boolean ours, left;
    static int steps, muted;
    static final RandomSource R = RandomSource.create();

    static boolean on() {
        return MazeHud.showing();
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        if (!on()) {
            fwd = side = 0;
            return;
        }
        var in = e.getInput();
        in.jumping = false;
        float k = in.shiftKeyDown ? 0.35f : 0.16f;
        fwd += (in.forwardImpulse - fwd) * (Math.abs(in.forwardImpulse) < Math.abs(fwd) ? 0.3f : k);
        side += (in.leftImpulse - side) * (Math.abs(in.leftImpulse) < Math.abs(side) ? 0.3f : k);
        if (Math.abs(fwd) < 0.01f) fwd = 0;
        if (Math.abs(side) < 0.01f) side = 0;
        in.forwardImpulse = fwd;
        in.leftImpulse = side * 0.8f;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !on()) {
            stride = 0;
            bobAmp = 0;
            return;
        }
        mc.options.keyJump.setDown(false);
        double dx = p.getX() - lx, dz = p.getZ() - lz;
        lx = p.getX();
        lz = p.getZ();
        double mv = Math.sqrt(dx * dx + dz * dz);
        if (mv > 1.5) mv = 0;
        boolean sprint = p.isSprinting(), crouch = p.isCrouching();
        lastBob = bob;
        bob += (float) mv * (sprint ? 2.6f : crouch ? 3.4f : 2.9f);
        float target = mv < 0.01 ? 0f : sprint ? 1.25f : crouch ? 0.45f : 0.8f;
        bobAmp += (target - bobAmp) * 0.15f;
        if (!p.onGround() || mv < 0.01) return;
        stride += mv;
        double len = sprint ? 2.2 : crouch ? 1.15 : 1.75;
        if (stride < len) return;
        stride = 0;
        steps++;
        step(mc, p, sprint, crouch);
    }

    static void step(Minecraft mc, LocalPlayer p, boolean sprint, boolean crouch) {
        BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        SoundType type = p.level().getBlockState(under).getSoundType();
        SoundEvent base = type.getStepSound();
        left = !left;
        float vol = crouch ? 0.09f : sprint ? 0.5f : 0.24f;
        float pitch = (crouch ? 0.5f : sprint ? 0.68f : 0.58f) + (left ? 0.03f : -0.03f) + R.nextFloat() * 0.06f;
        play(mc, p, base, vol, pitch);
        if (type == SoundType.WOOD || type == SoundType.STEM || type == SoundType.BAMBOO_WOOD) {
            if (R.nextFloat() < (sprint ? 0.35f : 0.18f)) play(mc, p, SoundEvents.WOODEN_TRAPDOOR_CLOSE, vol * 0.18f, 0.35f + R.nextFloat() * 0.1f);
            if (R.nextFloat() < 0.08f) play(mc, p, SoundEvents.LADDER_STEP, vol * 0.5f, 0.4f);
        } else {
            play(mc, p, SoundEvents.GRAVEL_STEP, vol * 0.25f, 0.5f + R.nextFloat() * 0.1f);
        }
        if (!crouch) play(mc, p, SoundEvents.ARMOR_EQUIP_LEATHER, vol * 0.22f, 0.7f + R.nextFloat() * 0.2f);
    }

    static void play(Minecraft mc, LocalPlayer p, SoundEvent s, float vol, float pitch) {
        ours = true;
        try {
            mc.getSoundManager().play(new SimpleSoundInstance(s, SoundSource.PLAYERS, vol, pitch, R, p.getX(), p.getY(), p.getZ()));
        } finally {
            ours = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sound(PlaySoundEvent e) {
        if (ours || !on()) return;
        SoundInstance s = e.getSound();
        if (s == null) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String path = s.getLocation().getPath(), ns = s.getLocation().getNamespace();
        boolean step = path.contains("step") || path.contains(".fall") || path.contains("land") || ns.equals("presencefootsteps");
        if (!step || s.getSource() == SoundSource.HOSTILE) return;
        double dx = s.getX() - p.getX(), dy = s.getY() - p.getY(), dz = s.getZ() - p.getZ();
        if (dx * dx + dy * dy + dz * dz < 6.25 || s.isRelative()) {
            e.setSound(null);
            muted++;
        }
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (!on() || bobAmp < 0.01f) return;
        float t = Mth.lerp((float) e.getPartialTick(), lastBob, bob);
        float a = bobAmp;
        e.setPitch(e.getPitch() + Math.abs(Mth.sin(t)) * 0.9f * a - 0.45f * a);
        e.setRoll(e.getRoll() + Mth.sin(t) * 0.7f * a);
        e.setYaw(e.getYaw() + Mth.cos(t) * 0.25f * a);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        if (!on()) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty()) e.setCanceled(true);
    }

    private MazeMove() {}
}
