package vn.cavemind.gear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

public final class Signal {
    static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.75f, 0.08f, 0.12f), 1.6f);

    public static void smoke(ServerPlayer p, BlockPos at, boolean crimson) {
        ServerLevel lvl = p.serverLevel();
        double x = at.getX() + 0.5, y = at.getY() + 0.8, z = at.getZ() + 0.5;
        lvl.sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x, y, z, 1, 0.05, 0.1, 0.05, 0.004);
        if (crimson) lvl.sendParticles(p, RED, true, x, y + 2, z, 6, 0.15, 2.0, 0.15, 0);
        if (p.distanceToSqr(x, y, z) < 40 * 40) lvl.sendParticles(p, Finder.TRAIL, true, x, y + 0.4, z, 3, 0.6, 0.5, 0.6, 0);
    }

    private Signal() {}
}
