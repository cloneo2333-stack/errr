package vn.cavemind.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// No sky down there. With the camera's night vision off, the lightmap keeps only the faint amber spill of the LCD,
// and the fog in ClientFx swallows everything past two metres, so you can feel along the wall but never see down the corridor.
final class MazeSky extends DimensionSpecialEffects {
    static final float R = 0.17f, G = 0.075f, B = 0.04f;

    MazeSky() {
        super(Float.NaN, true, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.scale(0.15);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float k = MazeHud.lcd();
        if (k <= 0.001f) return;
        colors.set(Math.max(colors.x(), R * k), Math.max(colors.y(), G * k), Math.max(colors.z(), B * k));
    }
}
