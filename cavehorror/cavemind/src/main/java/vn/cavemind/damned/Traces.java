package vn.cavemind.damned;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Watch;

final class Traces {
    static boolean run(ServerPlayer p, Watch w, Profile pr) {
        if (pr.home == null) return true;
        ServerLevel lvl = p.serverLevel();
        if (!lvl.isLoaded(pr.home)) return true;
        boolean asleep = p.isSleeping();
        boolean away = pr.home.distSqr(p.blockPosition()) > 48 * 48;
        if (!asleep && !away && !w.indoors) return false;
        RandomSource r = p.getRandom();
        int done = 0;
        List<BlockPos> lights = new ArrayList<>(), torches = new ArrayList<>();
        BlockPos h = pr.home;
        for (int dy = -4; dy <= 6; dy++) {
            for (int dx = -22; dx <= 22; dx++) {
                for (int dz = -22; dz <= 22; dz++) {
                    BlockPos q = h.offset(dx, dy, dz);
                    BlockState st = lvl.getBlockState(q);
                    if (st.isAir()) continue;
                    if (Indoors.lit(st)) lights.add(q);
                    else if (isTorch(st) && lvl.canSeeSky(q)) torches.add(q);
                }
            }
        }
        int n = 1 + r.nextInt(2);
        for (int i = 0; i < n && !lights.isEmpty(); i++) {
            BlockPos q = lights.remove(r.nextInt(lights.size()));
            if (!asleep && seen(p, w, q)) continue;
            BlockState st = lvl.getBlockState(q);
            lvl.setBlock(q, st.setValue(BlockStateProperties.LIT, false), 11);
            done++;
        }
        int t = pr.dStage >= 1 ? 1 + r.nextInt(2) : r.nextInt(2);
        for (int i = 0; i < t && !torches.isEmpty(); i++) {
            BlockPos q = torches.remove(r.nextInt(torches.size()));
            if (!asleep && seen(p, w, q)) continue;
            lvl.removeBlock(q, false);
            pr.dStolen++;
            pr.moment(new Moment(Moment.TOOK, Dread.day(lvl), Dread.time(lvl), q, 0, 1));
            done++;
        }
        List<ItemFrame> frames = lvl.getEntitiesOfClass(ItemFrame.class, new AABB(h).inflate(16, 6, 16), f -> !f.getItem().isEmpty());
        if (!frames.isEmpty() && r.nextFloat() < 0.6f) {
            ItemFrame f = frames.get(r.nextInt(frames.size()));
            if (asleep || !seen(p, w, f.blockPosition())) {
                f.setRotation(f.getRotation() + 2 + r.nextInt(4));
                done++;
            }
        }
        CaveMind.debug("night traces at {}'s home: {}", p.getGameProfile().getName(), done);
        return true;
    }

    static boolean isTorch(BlockState st) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        return id != null && id.getPath().contains("torch") && !id.getPath().contains("redstone");
    }

    private static boolean seen(ServerPlayer p, Watch w, BlockPos q) {
        Vec3 c = Vec3.atCenterOf(q);
        if (c.distanceToSqr(p.getEyePosition()) > 80 * 80) return false;
        Sight s = Sight.of(p, w, c);
        return s.on && s.edge() < 1.2 && Sight.clear(p.serverLevel(), p.getEyePosition(), c, p);
    }

    private Traces() {}
}
