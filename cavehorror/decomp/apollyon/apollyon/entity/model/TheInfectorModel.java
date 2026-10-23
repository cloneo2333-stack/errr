package apollyon.entity.model;

import apollyon.entity.TheInfectorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TheInfectorModel extends GeoModel<TheInfectorEntity> {
   public ResourceLocation getAnimationResource(TheInfectorEntity entity) {
      return new ResourceLocation("apollyon", "animations/theinfector.animation.json");
   }

   public ResourceLocation getModelResource(TheInfectorEntity entity) {
      return new ResourceLocation("apollyon", "geo/theinfector.geo.json");
   }

   public ResourceLocation getTextureResource(TheInfectorEntity entity) {
      return new ResourceLocation("apollyon", "textures/entities/" + entity.getTexture() + ".png");
   }
}
