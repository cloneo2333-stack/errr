package vn.cavemind.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vn.cavemind.CaveMind;
import vn.cavemind.net.Net;

@Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT)
public final class HeroClient {
    static final ResourceLocation EYES = new ResourceLocation(CaveMind.MODID, "textures/entity/hero_eyes.png");
    private static final Map<Integer, HeroEntity> LIVE = new HashMap<>();
    private static boolean wasThird;

    public static void on(Net.Hero h) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl == null || mc.player == null) return;
        HeroEntity e = LIVE.get(h.slot());
        switch (h.op()) {
            case Net.Hero.SPAWN -> {
                if (e != null) remove(lvl, e);
                e = new HeroEntity(lvl, new GameProfile(UUID.randomUUID(), "Herobrine"), h.slot());
                if ("self".equals(h.str())) {
                    e.skin = mc.player.getSkinTextureLocation();
                    e.model = mc.player.getModelName();
                } else if (h.str().startsWith("player:") && mc.getConnection() != null) {
                    net.minecraft.client.multiplayer.PlayerInfo info = null;
                    try {
                        info = mc.getConnection().getPlayerInfo(UUID.fromString(h.str().substring(7)));
                    } catch (IllegalArgumentException ignored) {
                    }
                    if (info != null) {
                        e.skin = info.getSkinLocation();
                        e.model = info.getModelName();
                    } else {
                        e.skin = new ResourceLocation("watching", "textures/item/herobrine/classic/classic.png");
                    }
                } else {
                    e.skin = ResourceLocation.tryParse(h.str());
                }
                e.setId(-77000 - h.slot());
                e.moveTo(h.x(), h.y(), h.z(), h.yaw(), h.pitch());
                e.setYHeadRot(h.head());
                e.setYBodyRot(h.yaw());
                e.yHeadRotO = h.head();
                e.yBodyRotO = h.yaw();
                state(e, h.flags());
                lvl.addPlayer(e.getId(), e);
                LIVE.put(h.slot(), e);
            }
            case Net.Hero.MOVE -> {
                if (e == null) return;
                int steps = (int) Math.max(1, h.data());
                e.lerpTo(h.x(), h.y(), h.z(), h.yaw(), h.pitch(), steps, false);
                e.lerpHeadTo(h.head(), steps);
            }
            case Net.Hero.STATE -> {
                if (e != null) state(e, h.flags());
            }
            case Net.Hero.ITEM -> {
                if (e == null) return;
                ResourceLocation id = ResourceLocation.tryParse(h.str());
                ItemStack st = id == null || h.str().isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
                if (st.is(Items.AIR)) st = ItemStack.EMPTY;
                e.setItemSlot(EquipmentSlot.MAINHAND, st);
            }
            case Net.Hero.SWING -> {
                if (e != null) e.swing(InteractionHand.MAIN_HAND);
            }
            case Net.Hero.SLEEP -> {
                if (e == null) return;
                e.startSleeping(BlockPos.of(h.data()));
            }
            case Net.Hero.WAKE -> {
                if (e == null) return;
                e.clearSleepingPos();
                e.setPose(Pose.STANDING);
                e.moveTo(h.x(), h.y(), h.z(), h.yaw(), h.pitch());
                e.setYHeadRot(h.head());
                e.setYBodyRot(h.yaw());
            }
            case Net.Hero.REMOVE -> {
                if (e != null) remove(lvl, e);
            }
            default -> {}
        }
    }

    private static void state(HeroEntity e, int flags) {
        e.eyes = (flags & 1) != 0;
        e.shoulder = (flags & 2) != 0;
        boolean crouch = (flags & 4) != 0;
        e.setShiftKeyDown(crouch);
        if (!e.isSleeping()) e.setPose(crouch ? Pose.CROUCHING : Pose.STANDING);
    }

    private static void remove(ClientLevel lvl, HeroEntity e) {
        LIVE.remove(e.slot);
        if (e.isSleeping()) e.clearSleepingPos();
        lvl.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || LIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean third = !mc.options.getCameraType().isFirstPerson();
        if (third && !wasThird) {
            for (HeroEntity e : LIVE.values()) {
                if (e.shoulder) Net.CH.sendToServer(new Net.HeroAct(Net.HeroAct.SEEN, e.slot));
            }
        }
        wasThird = third;
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Pre ev) {
        if (ev.getEntity() instanceof HeroEntity e && e.shoulder && Minecraft.getInstance().options.getCameraType().isFirstPerson()) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void click(InputEvent.InteractionKeyMappingTriggered ev) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof HeroEntity e)) return;
        if (!ev.isAttack() && !ev.isUseItem()) return;
        ev.setCanceled(true);
        ev.setSwingHand(ev.isAttack());
        Net.CH.sendToServer(new Net.HeroAct(Net.HeroAct.HIT, e.slot));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut ev) {
        LIVE.clear();
        wasThird = false;
    }

    @Mod.EventBusSubscriber(modid = CaveMind.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        @SubscribeEvent
        public static void add(EntityRenderersEvent.AddLayers ev) {
            for (String skin : ev.getSkins()) {
                if (ev.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new Eyes(r));
            }
        }
    }

    static final class Eyes extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Eyes(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, AbstractClientPlayer p, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
            if (!(p instanceof HeroEntity e) || !e.eyes || p.isInvisible()) return;
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(EYES));
            getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }

    private HeroClient() {}
}
