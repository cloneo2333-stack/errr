package vn.cavemind.damned;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.net.Net;
import vn.cavemind.world.Fx;

public final class Dark {
    static final int R = 40, MAX = 900;
    private static final Map<UUID, Out> OUT = new HashMap<>();

    record Out(ResourceKey<Level> dim, List<BlockPos> at) {}

    public static boolean on(ServerPlayer p) {
        return OUT.containsKey(p.getUUID());
    }

    public static void clear() {
        OUT.clear();
    }

    public static int start(ServerPlayer p) {
        if (on(p) || p.level().dimension() != Level.OVERWORLD) return 0;
        ServerLevel lvl = p.serverLevel();
        BlockPos c = p.blockPosition();
        List<BlockPos> out = new ArrayList<>();
        List<BlockPos> puff = new ArrayList<>();
        int y0 = c.getY() - 14, y1 = c.getY() + 14;
        for (int cx = (c.getX() - R) >> 4; cx <= (c.getX() + R) >> 4 && out.size() < MAX; cx++) {
            for (int cz = (c.getZ() - R) >> 4; cz <= (c.getZ() + R) >> 4 && out.size() < MAX; cz++) {
                LevelChunk ch = lvl.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                LevelChunkSection[] secs = ch.getSections();
                for (int i = 0; i < secs.length && out.size() < MAX; i++) {
                    LevelChunkSection sec = secs[i];
                    int sy = lvl.getSectionYFromSectionIndex(i) << 4;
                    if (sec.hasOnlyAir() || sy + 15 < y0 || sy > y1 || !sec.maybeHas(s -> s.getLightEmission() > 0)) continue;
                    for (int y = 0; y < 16; y++) {
                        int wy = sy + y;
                        if (wy < y0 || wy > y1) continue;
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = sec.getBlockState(x, y, z);
                                if (st.getLightEmission() <= 0) continue;
                                BlockPos q = new BlockPos((cx << 4) + x, wy, (cz << 4) + z);
                                if (q.distSqr(c) > R * R) continue;
                                BlockState f = fake(lvl, q, st);
                                if (f == null) continue;
                                p.connection.send(new ClientboundBlockUpdatePacket(q, f));
                                out.add(q);
                                if (q.distSqr(c) < 18 * 18 && puff.size() < 24) puff.add(q);
                            }
                        }
                    }
                }
            }
        }
        OUT.put(p.getUUID(), new Out(lvl.dimension(), out));
        for (BlockPos q : puff) {
            Vec3 v = Vec3.atCenterOf(q);
            lvl.sendParticles(p, ParticleTypes.SMOKE, true, v.x, v.y + 0.2, v.z, 6, 0.1, 0.15, 0.1, 0.01);
        }
        if (!puff.isEmpty()) Fx.sound(p, "minecraft:block.candle.extinguish", SoundSource.BLOCKS, Vec3.atCenterOf(puff.get(0)), 0.7f, 0.5f);
        Fx.sound(p, "minecraft:block.fire.extinguish", SoundSource.AMBIENT, p.position().add(0, 6, 0), 0.25f, 0.5f);
        Net.fx(p, Net.DARK, 1f, 50);
        return out.size();
    }

    static BlockState fake(ServerLevel lvl, BlockPos q, BlockState st) {
        if (!st.getFluidState().isEmpty() || st.is(BlockTags.CANDLES) || st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.LAVA_CAULDRON) || st.is(Blocks.MAGMA_BLOCK)) return null;
        if (st.hasProperty(BlockStateProperties.LIT)) return st.getValue(BlockStateProperties.LIT) ? st.setValue(BlockStateProperties.LIT, false) : null;
        Block b = st.getBlock();
        if (b == Blocks.JACK_O_LANTERN) return Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, st.getValue(CarvedPumpkinBlock.FACING));
        if (b instanceof TorchBlock || b instanceof LanternBlock || b == Blocks.END_ROD || b == Blocks.GLOW_LICHEN) return Blocks.AIR.defaultBlockState();
        if (st.getLightEmission() >= 10 && !st.hasBlockEntity() && !st.isCollisionShapeFullBlock(lvl, q)) return Blocks.AIR.defaultBlockState();
        return null;
    }

    public static BlockState fakeFor(ServerLevel lvl, BlockPos q, BlockState st) {
        return fake(lvl, q, st);
    }

    public static void end(ServerPlayer p) {
        Out o = OUT.remove(p.getUUID());
        if (o == null) return;
        Net.fx(p, Net.DARK, 0f, 70);
        if (p.level().dimension() != o.dim()) return;
        ServerLevel lvl = p.serverLevel();
        for (BlockPos q : o.at()) if (lvl.isLoaded(q)) p.connection.send(new ClientboundBlockUpdatePacket(lvl, q));
    }

    public static void forget(ServerPlayer p) {
        OUT.remove(p.getUUID());
    }

    static boolean covered(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        int n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= 2; k++) {
                BlockState s = lvl.getBlockState(head.relative(d, k));
                if (s.is(BlockTags.LEAVES) || s.canOcclude() || s.is(BlockTags.LOGS)) {
                    n++;
                    break;
                }
            }
        }
        BlockState up = lvl.getBlockState(head.above());
        if (up.is(BlockTags.LEAVES) || up.canOcclude()) n++;
        return n >= 3;
    }

    static boolean dark(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        int sky = lvl.getBrightness(LightLayer.SKY, eye) - Math.max(lvl.getSkyDarken(), 7);
        return sky <= 6 || covered(p);
    }

    private Dark() {}
}
