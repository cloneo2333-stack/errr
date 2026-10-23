package apollyon.entity.model;

import apollyon.entity.TheFallenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class TheFallenModel extends GeoModel<TheFallenEntity> {
   public ResourceLocation getAnimationResource(TheFallenEntity entity) {
      return new ResourceLocation("apollyon", "animations/thefallen.animation.json");
   }

   public ResourceLocation getModelResource(TheFallenEntity entity) {
      return new ResourceLocation("apollyon", "geo/thefallen.geo.json");
   }

   public ResourceLocation getTextureResource(TheFallenEntity entity) {
      return new ResourceLocation("apollyon", "textures/entities/" + entity.getTexture() + ".png");
   }

   public void setCustomAnimations(TheFallenEntity animatable, long instanceId, AnimationState animationState) {
      CoreGeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
         head.setRotY(entityData.netHeadYaw() * (float) (Math.PI / 180.0));
      }
   }
}
