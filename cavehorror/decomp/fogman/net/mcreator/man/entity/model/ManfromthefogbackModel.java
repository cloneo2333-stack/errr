package net.mcreator.man.entity.model;

import net.mcreator.man.entity.ManfromthefogbackEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ManfromthefogbackModel extends GeoModel<ManfromthefogbackEntity> {
   public ResourceLocation getAnimationResource(ManfromthefogbackEntity entity) {
      return new ResourceLocation("man", "animations/the_man_from_the_fog.animation.json");
   }

   public ResourceLocation getModelResource(ManfromthefogbackEntity entity) {
      return new ResourceLocation("man", "geo/the_man_from_the_fog.geo.json");
   }

   public ResourceLocation getTextureResource(ManfromthefogbackEntity entity) {
      return new ResourceLocation("man", "textures/entities/" + entity.getTexture() + ".png");
   }

   public void setCustomAnimations(ManfromthefogbackEntity animatable, long instanceId, AnimationState animationState) {
      CoreGeoBone head = this.getAnimationProcessor().getBone("h_head");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
