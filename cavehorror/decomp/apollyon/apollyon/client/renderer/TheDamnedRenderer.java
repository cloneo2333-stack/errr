package apollyon.client.renderer;

import apollyon.entity.TheDamnedEntity;
import apollyon.entity.layer.TheDamnedLayer;
import apollyon.entity.model.TheDamnedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TheDamnedRenderer extends GeoEntityRenderer<TheDamnedEntity> {
   public TheDamnedRenderer(Context renderManager) {
      super(renderManager, new TheDamnedModel());
      this.f_114477_ = 0.5F;
      this.addRenderLayer(new TheDamnedLayer(this));
   }

   public RenderType getRenderType(TheDamnedEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.m_110473_(this.m_5478_(animatable));
   }

   public void preRender(
      PoseStack poseStack,
      TheDamnedEntity entity,
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

   protected float getDeathMaxRotation(TheDamnedEntity entityLivingBaseIn) {
      return 0.0F;
   }
}
