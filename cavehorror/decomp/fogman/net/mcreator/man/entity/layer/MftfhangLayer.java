package net.mcreator.man.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.man.entity.MftfhangEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class MftfhangLayer extends GeoRenderLayer<MftfhangEntity> {
   private static final ResourceLocation LAYER = new ResourceLocation("man", "textures/entities/stalker_glow.png");

   public MftfhangLayer(GeoRenderer<MftfhangEntity> entityRenderer) {
      super(entityRenderer);
   }

   public void render(
      PoseStack poseStack,
      MftfhangEntity animatable,
      BakedGeoModel bakedModel,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      float partialTick,
      int packedLight,
      int packedOverlay
   ) {
      RenderType glowRenderType = RenderType.m_110488_(LAYER);
      this.getRenderer()
         .reRender(
            this.getDefaultBakedModel(animatable),
            poseStack,
            bufferSource,
            animatable,
            glowRenderType,
            bufferSource.m_6299_(glowRenderType),
            partialTick,
            packedLight,
            OverlayTexture.f_118083_,
            1.0F,
            1.0F,
            1.0F,
            1.0F
         );
   }
}
