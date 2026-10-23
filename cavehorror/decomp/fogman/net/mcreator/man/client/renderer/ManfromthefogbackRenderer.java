package net.mcreator.man.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.man.entity.ManfromthefogbackEntity;
import net.mcreator.man.entity.layer.ManfromthefogbackLayer;
import net.mcreator.man.entity.model.ManfromthefogbackModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ManfromthefogbackRenderer extends GeoEntityRenderer<ManfromthefogbackEntity> {
   public ManfromthefogbackRenderer(Context renderManager) {
      super(renderManager, new ManfromthefogbackModel());
      this.f_114477_ = 1.0F;
      this.addRenderLayer(new ManfromthefogbackLayer(this));
   }

   public RenderType getRenderType(ManfromthefogbackEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.m_110473_(this.m_5478_(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      ManfromthefogbackEntity entity,
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

   protected float getDeathMaxRotation(ManfromthefogbackEntity entityLivingBaseIn) {
      return 0.0F;
   }
}
