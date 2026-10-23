package vn.cavemind.hero;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.sense.Group;

public final class Ghosts {
    record Ghost(BlockPos pos, ResourceKey<Level> dim, long expires, double near) {}

    private static final Map<UUID, List<Ghost>> BY = new HashMap<>();

    public static boolean show(ServerPlayer p, BlockPos pos, BlockState fake, int ticks, double near) {
        ServerLevel lvl = p.serverLevel();
        BlockState real = lvl.getBlockState(pos);
        if (!fake.getCollisionShape(lvl, pos).isEmpty() || !real.getCollisionShape(lvl, pos).isEmpty()) return false;
        if (Group.seenByOthers(p, Vec3.atCenterOf(pos), 96)) return false;
        p.connection.send(new ClientboundBlockUpdatePacket(pos, fake));
        BY.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Ghost(pos.immutable(), lvl.dimension(), p.server.getTickCount() + ticks, near));
        return true;
    }

    public static boolean any(ServerPlayer p) {
        List<Ghost> l = BY.get(p.getUUID());
        return l != null && !l.isEmpty();
    }

    public static void tick(ServerPlayer p, long now) {
        if (now % 5 != 0) return;
        List<Ghost> list = BY.get(p.getUUID());
        if (list == null || list.isEmpty()) return;
        ServerLevel lvl = p.serverLevel();
        list.removeIf(g -> {
            boolean other = g.dim() != lvl.dimension();
            boolean done = other || now > g.expires() || p.position().distanceToSqr(Vec3.atCenterOf(g.pos())) < g.near() * g.near();
            if (done && !other) p.connection.send(new ClientboundBlockUpdatePacket(lvl, g.pos()));
            return done;
        });
    }

    public static void forget(UUID id) {
        BY.remove(id);
    }

    public static void clear() {
        BY.clear();
    }

    private Ghosts() {}
}
