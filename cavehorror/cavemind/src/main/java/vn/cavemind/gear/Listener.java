package vn.cavemind.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.maze.Cam;
import vn.cavemind.maze.Maze;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;

public final class Listener extends Gear.Thing {
    Listener() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level lvl, Player p, InteractionHand hand) {
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(p.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack st) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack st) {
        return UseAnim.NONE;
    }

    static void say(ServerPlayer p, String s) {
        p.displayClientMessage(Component.literal(s).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    public void onUseTick(Level lvl, LivingEntity e, ItemStack st, int left) {
        if (lvl.isClientSide || !(e instanceof ServerPlayer p)) return;
        int used = getUseDuration(st) - left;
        if (!Maze.inMaze(p)) {
            if (used == 1) say(p, "Chỉ có tiếng rè đều đều. Máy này chỉ bắt được sóng trong Giờ Thứ Bốn Mươi Tám.");
            if (used > 30) p.stopUsingItem();
            return;
        }
        if (Watch.of(p).speed > 0.02) {
            if (used % 20 == 1) say(p, "Đứng yên mới bắt được sóng.");
            Net.fx(p, Net.SCAN, 0f, 4);
            return;
        }
        if (!Cam.drain(p, Cam.DRAIN * 2)) {
            say(p, "Hết pin. Máy thu âm tắt ngóm.");
            Net.fx(p, Net.SCAN, 0f, 4);
            p.stopUsingItem();
            return;
        }
        Vec3 d = Maze.damned(p);
        float s = 0;
        if (d != null) {
            Vec3 to = d.subtract(p.getEyePosition());
            double dist = to.length();
            double ang = Math.toDegrees(Math.acos(Mth.clamp(p.getLookAngle().dot(to.normalize()), -1, 1)));
            if (dist < 16 && ang < 24) s = (float) ((1 - ang / 24) * (1 - dist / 18));
        }
        if (used % 2 == 0) Net.fx(p, Net.SCAN, Math.max(0.04f, s), 8);
        if (s > 0.05f && used % 3 == 0) Fx.sound(p, Compat.sound("simpleradio:short_circuit", "minecraft:block.fire.extinguish"), SoundSource.PLAYERS, p.position(), 0.12f + s * 0.4f, 1.3f + p.getRandom().nextFloat() * 0.3f);
        else if (used % 16 == 0) Fx.sound(p, "minecraft:block.beehive.work", SoundSource.PLAYERS, p.position(), 0.08f, 1.9f);
    }

    @Override
    public void releaseUsing(ItemStack st, Level lvl, LivingEntity e, int left) {
        if (!lvl.isClientSide && e instanceof ServerPlayer p) Net.fx(p, Net.SCAN, 0f, 4);
    }
}
