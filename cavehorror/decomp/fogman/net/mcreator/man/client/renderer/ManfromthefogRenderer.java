package net.mcreator.man.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.entity.layer.ManfromthefogLayer;
import net.mcreator.man.entity.model.ManfromthefogModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ManfromthefogRenderer extends GeoEntityRenderer<ManfromthefogEntity> {
   public ManfromthefogRenderer(Context renderManager) {
      super(renderManager, new ManfromthefogModel());
      this.f_114477_ = 1.0F;
      this.addRenderLayer(new ManfromthefogLayer(this));
   }

   public RenderType getRenderType(ManfromthefogEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.m_110473_(this.m_5478_(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      ManfromthefogEntity entity,
      BakedGeoModel model,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      float scale = 1.0F;
      this.scaleHeight = scale;
      this.scaleWidth = scale;
      super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   protected float getDeathMaxRotation(ManfromthefogEntity entityLivingBaseIn) {
      return 0.0F;
   }
}
