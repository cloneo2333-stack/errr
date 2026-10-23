package vn.cavemind.gear;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.maze.Maze;
import vn.cavemind.sense.Ears;
import vn.cavemind.world.Fx;

public final class Decoy extends Gear.Thing {
    public static final String TAG = "cavemind_decoy";

    Decoy() {
        super(new Item.Properties().stacksTo(4), 3);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level lvl, Player pl, InteractionHand hand) {
        ItemStack st = pl.getItemInHand(hand);
        if (lvl.isClientSide || !(pl instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
        if (Maze.inMaze(p) && !Maze.decoy(p)) {
            p.displayClientMessage(Component.literal("Mỗi ván dưới này chỉ dùng được hai viên.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.fail(st);
        }
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        ItemEntity ie = new ItemEntity(sl, eye.x + look.x * 0.4, eye.y - 0.25, eye.z + look.z * 0.4, st.copyWithCount(1));
        ie.setPickUpDelay(32767);
        ie.setDeltaMovement(look.x * 0.75, look.y * 0.75 + 0.15, look.z * 0.75);
        ie.getPersistentData().putBoolean(TAG, true);
        sl.addFreshEntity(ie);
        Fx.sound(p, "minecraft:entity.snowball.throw", SoundSource.PLAYERS, p.position(), 0.35f, 0.7f);
        if (!p.getAbilities().instabuild) st.shrink(1);
        p.getCooldowns().addCooldown(this, 40);
        UUID id = ie.getUUID();
        Fx.later(sl, 30, () -> rattle(p, sl, id, 0));
        return InteractionResultHolder.consume(st);
    }

    static void rattle(ServerPlayer p, ServerLevel lvl, UUID id, int n) {
        Entity e = lvl.getEntity(id);
        if (e == null) return;
        Vec3 at = e.position();
        if (n >= 16) {
            lvl.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.1, at.z, 6, 0.05, 0.05, 0.05, 0.01);
            Fx.soundAll(lvl, "minecraft:block.chain.break", SoundSource.NEUTRAL, at, 0.5f, 1.6f);
            e.discard();
            return;
        }
        var r = lvl.getRandom();
        Fx.soundAll(lvl, "minecraft:block.stone_button.click_on", SoundSource.NEUTRAL, at, 0.9f, 1.3f + r.nextFloat() * 0.4f);
        if (n % 3 == 0) Fx.soundAll(lvl, "minecraft:block.chain.hit", SoundSource.NEUTRAL, at, 0.6f, 1.5f);
        if (e.onGround()) e.setDeltaMovement((r.nextDouble() - 0.5) * 0.08, 0.16, (r.nextDouble() - 0.5) * 0.08);
        if (n % 2 == 0 && p.isAlive() && p.level() == lvl) Ears.noise(p, at, 18, Ears.Kind.TOY);
        Fx.later(lvl, 5, () -> rattle(p, lvl, id, n + 1));
    }
}
