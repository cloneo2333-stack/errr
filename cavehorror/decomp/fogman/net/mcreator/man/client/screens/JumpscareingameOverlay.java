package net.mcreator.man.client.screens;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.man.procedures.JumpscareoverlayingameProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.Render.Post;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class JumpscareingameOverlay {
   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void eventHandler(Post event) {
      if (event.getScreen() instanceof DeathScreen) {
         int w = event.getScreen().f_96543_;
         int h = event.getScreen().f_96544_;
         Level world = null;
         double x = 0.0;
         double y = 0.0;
         double z = 0.0;
         Player entity = Minecraft.m_91087_().f_91074_;
         if (entity != null) {
            world = entity.m_9236_();
            x = entity.m_20185_();
            y = entity.m_20186_();
            z = entity.m_20189_();
         }

         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (JumpscareoverlayingameProcedure.execute(world)) {
            event.getGuiGraphics().m_280163_(new ResourceLocation("man:textures/screens/the_man_from_the_fog.png"), 0, 0, 0.0F, 0.0F, w, h, w, h);
         }

         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }
}
