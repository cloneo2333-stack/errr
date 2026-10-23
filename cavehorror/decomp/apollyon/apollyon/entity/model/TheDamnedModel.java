package apollyon.entity.model;

import apollyon.entity.TheDamnedEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TheDamnedModel extends GeoModel<TheDamnedEntity> {
   public ResourceLocation getAnimationResource(TheDamnedEntity entity) {
      return new ResourceLocation("apollyon", "animations/thedamned.animation.json");
   }

   public ResourceLocation getModelResource(TheDamnedEntity entity) {
      return new ResourceLocation("apollyon", "geo/thedamned.geo.json");
   }

   public ResourceLocation getTextureResource(TheDamnedEntity entity) {
      return new ResourceLocation("apollyon", "textures/entities/" + entity.getTexture() + ".png");
   }
}
