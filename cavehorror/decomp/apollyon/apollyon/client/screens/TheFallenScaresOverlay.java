package apollyon.client.screens;

import apollyon.procedures.TheFallenConditionsProcedure;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Pre;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber({Dist.CLIENT})
public class TheFallenScaresOverlay {
   @SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public static void eventHandler(Pre event) {
      int w = event.getWindow().m_85445_();
      int h = event.getWindow().m_85446_();
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
      if (TheFallenConditionsProcedure.execute(entity)) {
         event.getGuiGraphics().m_280163_(new ResourceLocation("apollyon:textures/screens/the_fallens_jumpscare_overlay.png"), 0, 0, 0.0F, 0.0F, w, h, w, h);
      }

      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
