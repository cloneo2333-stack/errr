package vn.cavemind.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.gear.Barometer;
import vn.cavemind.gear.Gear;

public final class GearRenderer extends BlockEntityWithoutLevelRenderer {
    static final ResourceLocation BARO = rl("phong_vu_bieu_suong_base"), BARO_GLASS = rl("phong_vu_bieu_suong_glass"), BARO_FROST = rl("phong_vu_bieu_suong_frost"), NEEDLE = rl("phong_vu_bieu_suong_needle");
    static final ResourceLocation JAR = rl("lo_dom_dom_base"), FLY = rl("lo_dom_dom_fly");
    private static GearRenderer instance;
    static float needle = -110f;
    static long last;

    static ResourceLocation rl(String n) {
        return new ResourceLocation(CaveMind.MODID, "item/" + n);
    }

    public static GearRenderer get() {
        if (instance == null) instance = new GearRenderer();
        return instance;
    }

    GearRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static BakedModel model(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }

    static void draw(BakedModel m, ItemStack st, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft.getInstance().getItemRenderer().renderModelLists(m, st, light, overlay, ps, buf.getBuffer(Sheets.translucentCullBlockSheet()));
    }

    @Override
    public void renderByItem(ItemStack st, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (st.is(Gear.BARO.get())) barometer(st, ps, buf, light, overlay);
        else if (st.is(Gear.FINDER.get())) jar(st, ps, buf, light, overlay);
    }

    void barometer(ItemStack st, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        int lv = st.hasTag() ? st.getTag().getInt(Barometer.FOG) : 0;
        float v = st.hasTag() ? st.getTag().getFloat(Barometer.NEAR) : 0f;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - last) / 1000f);
        last = now;
        float target = -110f + 220f * Mth.clamp(v, 0f, 1f);
        needle += (target - needle) * Math.min(1f, dt * 2.5f);
        float shiver = lv == 2 ? Mth.sin(now / 23f) * 7f + Mth.sin(now / 9f) * 3f : lv == 1 ? Mth.sin(now / 61f) * 1.6f : Mth.sin(now / 900f) * 0.6f;
        draw(model(BARO), st, ps, buf, light, overlay);
        ps.pushPose();
        ps.translate(0.5, 0.5, 9.2 / 16.0);
        ps.mulPose(Axis.ZN.rotationDegrees(needle + shiver));
        quad(ps, buf, -0.022f, -0.05f, 0.022f, 0.285f, 0xFF2A1E18, light, overlay);
        quad(ps, buf, -0.024f, 0.2f, 0.024f, 0.29f, 0xFFB02418, light, overlay);
        ps.popPose();
        draw(model(lv == 2 ? BARO_FROST : BARO_GLASS), st, ps, buf, light, overlay);
    }

    static final ResourceLocation WHITE = new ResourceLocation(CaveMind.MODID, "textures/misc/white.png");

    static void quad(PoseStack ps, MultiBufferSource buf, float x0, float y0, float x1, float y1, int argb, int light, int overlay) {
        var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(WHITE));
        var m = ps.last().pose();
        var n = ps.last().normal();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        float z = 0.002f;
        vc.vertex(m, x0, y0, z).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(light).normal(n, 0, 0, 1).endVertex();
    }

    void jar(ItemStack st, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        draw(model(JAR), st, ps, buf, light, overlay);
        Minecraft mc = Minecraft.getInstance();
        float cool = mc.player == null ? 0f : mc.player.getCooldowns().getCooldownPercent(Gear.FINDER.get(), 0f);
        int n = Math.round(7 * (1f - cool));
        double t = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < n; i++) {
            double a = t * (0.6 + i * 0.13) + i * 2.1;
            double x = 8 + Math.cos(a) * (1.4 + (i % 3) * 0.8);
            double z = 8 + Math.sin(a * 1.1) * (1.4 + (i % 2) * 0.9);
            double y = 2.2 + (i * 1.15) % 7 + Math.sin(t * 1.7 + i) * 0.8;
            float blink = 0.55f + 0.45f * Mth.sin((float) (t * (2.2 + i * 0.4) + i));
            ps.pushPose();
            ps.translate(x / 16.0, y / 16.0, z / 16.0);
            float s = 0.5f + 0.5f * blink;
            ps.scale(s, s, s);
            ps.translate(-0.5, -0.5, -0.5);
            draw(model(FLY), st, ps, buf, LightTexture.FULL_BRIGHT, overlay);
            ps.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Models {
        @SubscribeEvent
        public static void extra(ModelEvent.RegisterAdditional e) {
            for (ResourceLocation r : new ResourceLocation[]{BARO, BARO_GLASS, BARO_FROST, NEEDLE, JAR, FLY}) e.register(r);
        }
    }
}
