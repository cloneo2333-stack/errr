package vn.cavemind.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import vn.cavemind.chapter.Book;
import vn.cavemind.chapter.Chapters;
import vn.cavemind.lore.Lore;
import vn.cavemind.lore.Points;
import vn.cavemind.memory.MindData;
import vn.cavemind.memory.Profile;
import vn.cavemind.world.Fx;

public final class Finder extends Gear.Thing {
    static final String MODE = "cavemind_mode";
    static final String[] MODES = {"tự chọn", "trang nhật ký", "sân chơi bỏ hoang", "điểm hẹn của chương", "nơi chưa ai tới"};
    static final String[] SITE = {"s1", "g", "s2", "h", "s3", "m", "p"};
    static final String[] SITE_VI = {"trạm đo sương số một", "khoảng rừng trống", "trạm đo sương số hai", "ngôi nhà bỏ hoang", "trạm đo sương số ba", "mỏ bỏ hoang", "sân chơi bỏ hoang"};
    static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(0.86f, 0.95f, 0.38f), 0.9f);
    static final DustParticleOptions HEAD = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.62f), 1.3f);
    static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.95f, 0.88f, 0.32f), 0.45f);
    static final DustParticleOptions TAIL = new DustParticleOptions(new Vector3f(0.55f, 0.7f, 0.22f), 0.3f);

    Finder() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 3);
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

    public record Target(BlockPos pos, String name) {}

    static Target site(ServerLevel ow, Profile pr, String k, String vi, boolean any) {
        BlockPos at = Lore.site(ow, k);
        if (at == null || !any && pr.lore.contains(k)) return null;
        return new Target(at, vi);
    }

    static Target chapter(ServerLevel ow, Profile pr) {
        int g = pr.chapter + 1;
        if (g > 6 || !Chapters.has(pr, g, Chapters.READY) || !Chapters.PLACE[g]) return null;
        Points.Point way = Points.get(ow, "w" + g), pt = Points.get(ow, "g" + g);
        if (way != null && !Chapters.has(pr, g, Chapters.WAY) && !Chapters.has(pr, g, Chapters.ARRIVED)) return new Target(way.target, "cây đếm giữa đường");
        if (pt != null && !Chapters.has(pr, g, Chapters.WON)) return new Target(pt.pos != null ? pt.pos : pt.target, "điểm hẹn của " + Points.CHILD[g]);
        return null;
    }

    static Target nearestUnseen(ServerLevel ow, Profile pr, ServerPlayer p) {
        Target best = null;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < SITE.length; i++) {
            Target t = site(ow, pr, SITE[i], SITE_VI[i], false);
            if (t == null) continue;
            double d = t.pos().distSqr(p.blockPosition());
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        return best;
    }

    public static Target target(ServerPlayer p, int mode) {
        ServerLevel ow = p.server.overworld();
        Profile pr = MindData.profile(p);
        Target page = pr.pageAt == null ? null : new Target(pr.pageAt, "trang nhật ký rách");
        return switch (mode) {
            case 1 -> page;
            case 2 -> site(ow, pr, "p", "sân chơi bỏ hoang", true);
            case 3 -> chapter(ow, pr);
            case 4 -> nearestUnseen(ow, pr, p);
            default -> {
                Target t = chapter(ow, pr);
                if (t == null) t = site(ow, pr, "p", "sân chơi bỏ hoang", false);
                if (t == null) t = page;
                if (t == null) t = nearestUnseen(ow, pr, p);
                yield t;
            }
        };
    }

    static void say(ServerPlayer p, String s) {
        p.displayClientMessage(Component.literal(s).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level lvl, Player pl, InteractionHand hand) {
        ItemStack st = pl.getItemInHand(hand);
        if (lvl.isClientSide || !(pl instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
        int mode = st.getOrCreateTag().getInt(MODE);
        if (p.isShiftKeyDown()) {
            mode = (mode + 1) % MODES.length;
            st.getOrCreateTag().putInt(MODE, mode);
            say(p, "Lọ đom đóm sẽ tìm: " + MODES[mode]);
            Fx.sound(p, "minecraft:item.bottle.fill", SoundSource.PLAYERS, p.position(), 0.4f, 1.6f);
            return InteractionResultHolder.success(st);
        }
        if (p.level().dimension() != Level.OVERWORLD) {
            say(p, "Đom đóm không chịu bay ra khỏi lọ ở nơi này.");
            return InteractionResultHolder.fail(st);
        }
        Target t = target(p, mode);
        if (t == null && Boolean.getBoolean("cavemind.clienttest")) t = new Target(p.blockPosition().offset((int) (p.getLookAngle().x * 40), 0, (int) (p.getLookAngle().z * 40)), "mục tiêu thử");
        ServerLevel sl = p.serverLevel();
        Fx.sound(p, "minecraft:item.bottle.empty", SoundSource.PLAYERS, p.position(), 0.5f, 1.4f);
        if (t == null) {
            circle(p, sl);
            say(p, "Đom đóm bay một vòng rồi chui lại vào lọ. Chưa có " + (mode == 0 ? "gì" : MODES[mode]) + " để tìm.");
            p.getCooldowns().addCooldown(this, 60);
            return InteractionResultHolder.success(st);
        }
        double d = Math.sqrt(t.pos().distSqr(p.blockPosition()));
        say(p, "Đom đóm bay đi tìm " + t.name() + " (" + Book.band(d) + ").");
        fly(p, sl, Vec3.atCenterOf(t.pos()));
        p.getCooldowns().addCooldown(this, 240);
        return InteractionResultHolder.success(st);
    }

    static void circle(ServerPlayer p, ServerLevel lvl) {
        Vec3 c = p.getEyePosition();
        for (int i = 0; i < 24; i++) {
            int k = i;
            Fx.later(lvl, i, () -> {
                double a = k / 24.0 * Math.PI * 2;
                lvl.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, true, c.x + Math.cos(a) * 2.6, c.y - 0.5 + Math.sin(k * 0.5) * 0.2, c.z + Math.sin(a) * 2.6, 0, 0, 0, 0, 0);
            });
        }
    }

    static int floor(ServerLevel lvl, int x, int y, int z) {
        for (int dy = 2; dy >= -6; dy--) {
            BlockPos q = new BlockPos(x, y + dy, z);
            if (!lvl.getBlockState(q).getCollisionShape(lvl, q).isEmpty() && lvl.getBlockState(q.above()).getCollisionShape(lvl, q.above()).isEmpty()) return y + dy + 1;
        }
        return y;
    }

    static void fly(ServerPlayer p, ServerLevel lvl, Vec3 to) {
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6));
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        double dist = flat.length();
        int steps = (int) Math.min(54, Math.max(6, dist / 0.6));
        Vec3 dir = dist < 1e-3 ? Vec3.ZERO : flat.normalize().scale(Math.min(0.6, dist / steps));
        double y = from.y;
        Vec3 last = from;
        for (int i = 1; i <= steps; i++) {
            Vec3 q = from.add(dir.scale(i));
            int gy = floor(lvl, (int) Math.floor(q.x), (int) Math.floor(y - 1.6), (int) Math.floor(q.z));
            double want = dist - i * dir.length() < 3 ? to.y : Mth.clamp(y, gy + 1.3, gy + 2.1);
            y += (want - y) * 0.35;
            double wob = Math.sin(i * 0.9) * 0.25;
            Vec3 at = new Vec3(q.x + dir.z * wob, y + Math.sin(i * 1.3) * 0.15, q.z - dir.x * wob);
            Vec3 prev = last;
            last = at;
            int k = i;
            if (i * dir.length() < 2.5) continue;
            Fx.later(lvl, i * 2, () -> {
                for (int s = 0; s < 3; s++) {
                    Vec3 m = prev.lerp(at, s / 3.0);
                    lvl.sendParticles(p, ParticleTypes.END_ROD, true, m.x, m.y, m.z, 0, 0, 0, 0, 0);
                }
                lvl.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, true, at.x, at.y, at.z, 0, 0, 0, 0, 0);
                lvl.sendParticles(p, ParticleTypes.END_ROD, true, at.x, at.y + 0.04, at.z, 0, 0, 0, 0, 0);
            });
        }
        Vec3 end = last;
        if (Boolean.getBoolean("cavemind.clienttest")) vn.cavemind.CaveMind.LOG.info("[CLIENTTEST] firefly from {} to {} ends {} look {} yaw {}", from, to, end, p.getLookAngle(), p.getYRot());
        Fx.later(lvl, steps * 2, () -> {
            for (int i = 0; i < 30; i++) {
                int k = i;
                Fx.later(lvl, i * 2, () -> {
                    double a = k * 0.5;
                    lvl.sendParticles(p, ParticleTypes.END_ROD, true, end.x + Math.cos(a) * 0.5, end.y + Math.sin(k * 0.3) * 0.2, end.z + Math.sin(a) * 0.5, 0, 0, 0, 0, 0);
                    lvl.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, true, end.x + Math.cos(a - 0.4) * 0.5, end.y + Math.sin(k * 0.3 - 0.2) * 0.2, end.z + Math.sin(a - 0.4) * 0.5, 0, 0, 0, 0, 0);
                });
            }
        });
        Fx.sound(p, "minecraft:entity.bee.pollinate", SoundSource.PLAYERS, from, 0.25f, 1.9f);
    }
}
